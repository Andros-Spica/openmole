package org.openmole.web.mole

import scala.reflect.ClassTag
import scala.io.{ Codec, Source }
import java.io._
import org.openmole.core.serializer.SerialiserService
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import org.openmole.core.model.mole.{ IPartialMoleExecution, IMoleExecution, ExecutionContext }
import org.openmole.misc.tools.io.FromString
import org.openmole.core.implementation.validation.DataflowProblem.MissingSourceInput
import org.openmole.core.model.data.{ Context, Prototype, Variable }
import org.openmole.core.implementation.validation.Validation
import javax.sql.rowset.serial.{ SerialBlob, SerialClob }
import org.openmole.misc.eventdispatcher.{ EventListener, EventDispatcher }
import akka.actor.ActorSystem

import resource._

import slick.driver.H2Driver.simple._
import slick.jdbc.meta.MTable

import Database.threadLocalSession
import org.openmole.misc.workspace.Workspace
import com.ice.tar.{ TarInputStream, Tar }
import scala.Some
import org.openmole.core.implementation.validation.DataflowProblem.MissingSourceInput
import org.openmole.core.implementation.validation.DataflowProblem.MissingInput
import org.scalatra.ScalatraBase
import org.openmole.web.db.tables.{ MoleData, MoleStats }
import org.openmole.web.cache.DataHandler
import org.openmole.web.db.{SlickDB, SlickSupport}

trait MoleHandling { self: ScalatraBase ⇒
  def system: ActorSystem
  val database: SlickDB
  val db = database.db

  protected implicit def executor: concurrent.ExecutionContext = system.dispatcher


  private val listener: EventListener[IMoleExecution] = new JobEventListener(moleStats, mole2CacheId)
  private val mStatusListener = new MoleStatusListener(this)

  db withSession {
    if (MTable.getTables("MoleData").list().isEmpty)
      MoleData.ddl.create // check that table exists somehow
  }

  db withSession {
    if (MTable.getTables("MoleStats").list().isEmpty)
      MoleStats.ddl.create
  }

  (getUnfinishedMoleKeys map getMole).flatten foreach (_.start)

  def getStatus(moleId: String): String =
    db withSession {
      (for (m ← MoleData if m.id === moleId) yield m.state).list().headOption.getOrElse("Doesn't Exist")
    }

  private def processXMLFile[A: ClassTag](is: Option[InputStream]): Either[A, String] = is match {
    case Some(stream) ⇒
      try {
        val ret = SerialiserService.deserialise[A](stream)
        Left(ret)
      }
      catch { //TODO: Make error messages more verbose
        case e: CannotResolveClassException ⇒ Right("The uploaded xml was not a valid serialized object.")
        case c: ClassCastException          ⇒ Right("Blargh")
        case e: Exception                   ⇒ Right("Could not parse the given mole")
      }
    case None ⇒ Right("No data was uploaded..")
  }

  private def processPack(is: Option[InputStream]): (Either[IPartialMoleExecution, String], Option[File]) = is match {
    case Some(stream) ⇒
      try {
        val p = Workspace.newDir // Todo: make sure that the encapsulate flag is implicit for packs
        val ret = managed(new TarInputStream(stream)) acquireAndGet { SerialiserService.deserialiseAndExtractFiles[IPartialMoleExecution](_, p) }
        Left(ret) -> Some(p)
      }
      catch {
        case e: CannotResolveClassException ⇒ Right("The uploaded pack was not a valid tar file") -> None
        case c: ClassCastException          ⇒ Right("Blargh") -> None
        case e: Exception                   ⇒ Right("Could not parse the given mole") -> None
      }
    case None ⇒ Right("No data was uploaded") -> None
  }

  private def reifyCSV(mole: IPartialMoleExecution, csvData: Map[String, String]) = {
    def fromString[T: FromString](s: String) = {
      implicitly[FromString[T]].fromString(s)
    }

    def createVariable[T: FromString](mI: MissingInput) = csvData get mI.data.prototype.name map (d ⇒ Variable[T](mI.data.prototype.asInstanceOf[Prototype[T]], fromString[T](d)))

    val a = Validation(mole.mole, sources = mole.sources, hooks = mole.hooks)
    val mIS = a.map(_ match {
      case x: MissingInput       ⇒ x
      case y: MissingSourceInput ⇒ MissingInput(y.slot, y.input)
      case error                 ⇒ throw new Exception(s"Malformed partial mole: $error")
    })

    val c = mIS.map {
      mI ⇒
        mI.data.prototype.`type`.erasure match {
          case t if t.equals(classOf[Int])    ⇒ createVariable[Int](mI)
          case t if t.equals(classOf[Double]) ⇒ createVariable[Double](mI)
          case t if t.equals(classOf[Float])  ⇒ createVariable[Float](mI)
          case t if t.equals(classOf[BigInt]) ⇒ createVariable[BigInt](mI)
          case t if t.equals(classOf[String]) ⇒ createVariable[String](mI)
          case t if t.equals(classOf[File])   ⇒ createVariable[File](mI)
          case _                              ⇒ throw new Exception(s"The missing parameter type: ${mI.data.prototype.`type`} is not known to the reification system.")
        }
    }

    if (!mIS.isEmpty && c.isEmpty) throw new Exception("No parameters given")

    Context(c.map(_.getOrElse(throw new Exception("CSV file does not have data on all missing variables"))))
  }

  private def createMoleExecution(pMole: IPartialMoleExecution, ctxt: Context, encapsulated: Boolean, mPath: Option[File] = None) = {
    val path: Option[File] = mPath orElse (if (encapsulated) Some(Workspace.newDir("")) else None)
    val context = ExecutionContext(new PrintStream(new File(path.getOrElse(".") + "/out")), path)
    val mole = pMole.toExecution(ctxt, context)

    EventDispatcher.listen(mole, listener, classOf[IMoleExecution.JobStatusChanged])
    EventDispatcher.listen(mole, listener, classOf[IMoleExecution.JobCreated])
    EventDispatcher.listen(mole, mStatusListener, classOf[IMoleExecution.Starting])
    EventDispatcher.listen(mole, mStatusListener, classOf[IMoleExecution.Finished])
    (mole, path)
  }

  private def cacheMoleExecution(moleExecution: IMoleExecution, path: Option[File], cacheId: String) = {
    path foreach (capsules.add(cacheId, _))
    cachedMoles.add(cacheId, moleExecution)
    mole2CacheId add (moleExecution, cacheId)
    moleExecution
  }

  private def regenDir(file: File) = {
    if (!file.exists()) file.mkdir()
  }

  def createMole(moleInput: ⇒ Option[InputStream], csvInput: ⇒ Option[InputStream], encapsulate: Boolean = false, pack: Boolean = false, name: String = ""): Either[String, IMoleExecution] = {
    val r = csvInput map Source.fromInputStream

    val regex = """(.*),(.*)""".r
    lazy val csvData = r.map(_.getLines().map(_ match {
      case regex(name: String, data: String) ⇒ name -> data
      case _                                 ⇒ throw new Exception("Invalidly formatted csv file")
    }).toMap) getOrElse Map()

    val moleBinary = moleInput map { str ⇒ { val arr = Source.fromInputStream(str)(Codec.ISO8859).toArray; str.close(); arr } }

    val moleStream = moleBinary map (b ⇒ new ByteArrayInputStream(b map (_.toByte)))

    val (moleExec, genPath) = if (pack) processPack(moleStream) else (processXMLFile[IPartialMoleExecution](moleStream), None)

    moleExec match {
      case Left(pEx) ⇒ {
        val ctxt = reifyCSV(pEx, csvData)

        val clob = new SerialClob(moleBinary.get)

        val ctxtClob = new SerialClob(SerialiserService.serialise(ctxt).toCharArray)

        val outputBlob = new SerialBlob(Array[Byte]())
        //val id = UUID.randomUUID().toString

        val (me, path) = createMoleExecution(pEx, ctxt, encapsulate, genPath)
        db withSession {
          MoleData.insert((me.id, name, MoleHandling.Status.stopped, clob, ctxtClob, encapsulate, pack, outputBlob))
        }
        cacheMoleExecution(me, path, me.id)
        Right(me)
      }
      case Right(error) ⇒ Left(error)
    }
  }

  def getMoleKeys = db withSession {
    (for {
      m ← MoleData
    } yield m.id.asColumnOf[String]).list
  }

  private def getUnfinishedMoleKeys = db withSession {
    (for (m ← MoleData if m.state === "Running") yield m.id.asColumnOf[String]).list
  }

  def getMole(key: String): Option[IMoleExecution] = {
    lazy val mole: Option[IMoleExecution] = db withSession {
      /*val f = new File((for (m ← MoleData if m.id === key) yield m.path).list.head)
      f.createNewFile()*/

      val row = MoleData filter (_.id === key)
      val molePack = (row map (_.molePackage)).list.headOption.getOrElse(false)
      val workDir = if (molePack) Some(Workspace.newDir) else None
      val moleDeserialiser: InputStream ⇒ IPartialMoleExecution = workDir map (dir ⇒
        (in: InputStream) ⇒ SerialiserService.deserialiseAndExtractFiles[IPartialMoleExecution](new TarInputStream(in), dir)
      ) getOrElse (SerialiserService.deserialise[IPartialMoleExecution](_))

      val r = (row map (r ⇒ (r.clobbedMole, r.clobbedContext, r.encapsulated))).list.headOption map {
        case (pMClob, ctxtClob, e) ⇒ (moleDeserialiser(pMClob.getAsciiStream), SerialiserService.deserialise[Context](ctxtClob.getAsciiStream), e)
      }

      r map Function.tupled(createMoleExecution(_, _, _, workDir)) map Function.tupled(cacheMoleExecution(_, _, key))
    }

    cachedMoles get key orElse mole
  }

  def getMoleResult(key: String) = db withSession {
    val blob = (for (m ← MoleData if m.id === key) yield m.result).list.head
    blob.getBytes(1, blob.length.toInt)
  }

  def getMoleStats(key: String) = {
    moleStats get key getOrElse MoleStats.empty
  }
  def startMole(key: String) { getMole(key) foreach (_.start) }

  def deleteMole(key: String) = {
    val ret = cachedMoles get key map (_.cancel)
    cachedMoles remove key
    db withSession {
      MoleData.filter(_.id === key).delete
    }

    ret
  }

  //Called automatically when execution is complete.
  def decacheMole(mole: IMoleExecution) = {
    val mKey = mole2CacheId get mole
    mKey foreach (id ⇒ println(s"decaching mole id: $id"))
    mKey foreach (cachedMoles get _ foreach (_.cancel))
    mKey foreach (k ⇒ List(cachedMoles, /*moleStats,*/ capsules) map (_ remove k)) //TODO: Commit the mole stats to the db so they can be retrieved after decaching.
    mole2CacheId remove mole
  }

  //todo fix to remove decached moles
  def setStatus(mole: IMoleExecution, status: String) = {
    db withSession {
      val moleId = mole2CacheId.get(mole).get
      val x = for { m ← MoleData if m.id === moleId } yield m.state
      x update status
      println(s"updated mole: ${moleId} to ${status}")
    }
  }

  def isEncapsulated(key: String): Boolean = db withSession {
    println(key)
    (for { m ← MoleData if m.id === key } yield m.encapsulated).list.forall(b ⇒ b)
  }

  //TODO - FRAGILE
  def storeResultBlob(exec: IMoleExecution) = db withSession {
    val moleId = mole2CacheId.get(exec).get
    println("starting the store op")
    val mPath = capsules get moleId
    for (path ← mPath) {
      println(path)
      val outFile = new File(Workspace.newFile.toString + ".tar")
      outFile.createNewFile()
      println(outFile)

      try {
        Tar.createDirectoryTar(path, outFile)

        for (tis ← managed(Source.fromFile(outFile)(Codec.ISO8859))) {
          val r = for (m ← MoleData if m.id === moleId) yield m.result

          val arr = tis.iter.toArray.map(_.toByte)
          val blob = new SerialBlob(arr)
          r.update(blob)
        }
      }
      catch {
        case e: Exception ⇒ e.printStackTrace(System.out)
      }
    }
  }
}

object MoleHandling {
  object Status {
    val running = "Running"
    val finished = "Finished"
    val stopped = "Stopped"
  }
}
