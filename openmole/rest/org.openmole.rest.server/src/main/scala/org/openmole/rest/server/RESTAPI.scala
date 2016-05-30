package org.openmole.rest.server

import java.io.{ File, PrintStream }
import java.util.UUID
import java.util.logging.Level
import java.util.zip.{ GZIPOutputStream, GZIPInputStream }
import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest
import org.json4s.JsonDSL._
import org.json4s._
import org.openmole.core.project._
import org.openmole.core.event._
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.core.workflow.mole.{ MoleExecution, MoleExecutionContext }
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workspace.{ Persistent, Workspace }
import org.openmole.tool.tar.{ TarOutputStream, TarInputStream }
import org.scalatra._
import org.scalatra.servlet.FileUploadSupport
import org.openmole.rest.message._
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.tool.tar._
import scala.util.{ Try, Failure, Success }
import org.openmole.tool.collection._
import org.json4s.jackson.JsonMethods._

case class EnvironmentException(environment: Environment, error: Error)

case class Execution(
  workDirectory: WorkDirectory,
  moleExecution: MoleExecution
)

case class WorkDirectory(workDirectory: File) {

  val output = workDirectory.newFile("output", ".txt")
  lazy val outputStream = new PrintStream(output.bufferedOutputStream())

  def readOutput = {
    outputStream.flush
    output.content
  }

  def clean = {
    outputStream.close
    workDirectory.recursiveDelete
  }

}

@MultipartConfig(fileSizeThreshold = 1024 * 1024) //research scala multipart config
trait RESTAPI extends ScalatraServlet with GZipSupport
    with FileUploadSupport
    with FlashMapSupport
    with Authentication {

  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal
  private val logger = Log.log

  private lazy val moles = DataHandler[ExecutionId, Execution]()

  implicit class ToJsonDecorator(x: Any) {
    def toJson = pretty(Extraction.decompose(x))
  }

  def arguments: RESTLifeCycle.Arguments
  def baseDirectory = Workspace.location / "rest"

  def exceptionToHttpError(e: Throwable) = InternalServerError(Error(e).toJson)

  post("/token") {
    Try(params("password")) map issueToken match {
      case Failure(_) ⇒ ExpectationFailed(Error("No password sent with request").toJson)
      case Success(Failure(InvalidPasswordException(msg))) ⇒ Forbidden(Error(msg).toJson)
      case Success(Failure(e)) ⇒ exceptionToHttpError(e)
      case Success(Success(AuthenticationToken(token, start, end))) ⇒ Accepted(Token(token, end - start).toJson)
    }
  }

  post("/start") {
    authenticate()
    (params get "script") match {
      case None ⇒ ExpectationFailed(Error("Missing mandatory script parameter.").toJson)
      case Some(script) ⇒
        logger.info("starting the create operation")

        val id = ExecutionId(UUID.randomUUID().toString)
        val directory = WorkDirectory(baseDirectory / id.id)

        def extract =
          for {
            archive ← fileParams get "workDirectory"
          } {
            val is = new TarInputStream(new GZIPInputStream(archive.getInputStream))
            try is.extract(directory.workDirectory) finally is.close
          }

        def error(e: Throwable) = {
          directory.clean
          ExpectationFailed(Error(e).toJson)
        }

        def start(ex: MoleExecution) = {
          Try(ex.start) match {
            case Failure(e) ⇒ error(e)
            case Success(ex) ⇒
              moles.add(id, Execution(directory, ex))
              Ok(id.toJson)
          }
        }

        val project = new Project(directory.workDirectory)
        project.compile(directory.workDirectory / script, Seq.empty) match {
          case ScriptFileDoesNotExists() ⇒ ExpectationFailed(Error("The script doesn't exist").toJson)
          case e: CompilationError       ⇒ error(e.error)
          case compiled: Compiled ⇒
            Try(compiled.eval) match {
              case Success(res) ⇒
                Try(res.buildPuzzle.toExecution(executionContext = MoleExecutionContext(out = directory.outputStream))) match {
                  case Success(ex) ⇒
                    ex listen { case (ex, ev: MoleExecution.Finished) ⇒ }
                    start(ex)
                  case Failure(e) ⇒ error(e)
                }
              case Failure(e) ⇒ error(e)
            }

        }
    }

  }

  post("/download") {
    authenticate()
    getExecution { ex ⇒
      val path = (params get "path").getOrElse("")
      val file = ex.workDirectory.workDirectory / path
      val gzOs = response.getOutputStream.toGZ

      if (file.isDirectory) {
        val os = new TarOutputStream(gzOs)
        contentType = "application/octet-stream"
        response.setHeader("Content-Disposition", "attachment; filename=" + "archive.tgz")
        os.archive(file)
        os.close
      }
      else {
        file.copy(gzOs)
      }
      Ok()
    }
  }

  post("/output") {
    authenticate()
    getExecution { ex ⇒ Ok(Output(ex.workDirectory.readOutput).toJson) }
  }

  post("/state") {
    authenticate()
    getExecution { ex ⇒
      val moleExecution = ex.moleExecution
      val state: State = (moleExecution.exception, moleExecution.finished) match {
        case (Some(t), _) ⇒ Failed(Error(t.exception).copy(message = s"Mole execution failed when execution capsule: ${t.capsule}"))
        case (None, true) ⇒ Finished()
        case _ ⇒
          def environments = moleExecution.environments.values.toSeq
          def environmentStatus = environments.map {
            env ⇒
              def environmentErrors = env.clearErrors.map(e ⇒ Error(e.exception).copy(level = Some(e.level.toString)))
              EnvironmentStatus(name = env.name, submitted = env.submitted, running = env.running, done = env.done, failed = env.failed, environmentErrors)
          }
          val statuses = moleExecution.jobStatuses
          Running(statuses.ready, statuses.running, statuses.completed, environmentStatus)
      }
      Ok(state.toJson)
    }
  }

  post("/remove") {
    authenticate()
    getId {
      moles.remove(_) match {
        case None ⇒ ExpectationFailed(Error("Execution not found").toJson)
        case Some(ex) ⇒
          ex.moleExecution.cancel
          ex.workDirectory.clean
          Ok()
      }
    }

  }

  post("/list") {
    authenticate()
    Ok(moles.getKeys.toSeq.toJson)
  }

  def getExecution(success: Execution ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult =
    getId {
      moles.get(_) match {
        case None     ⇒ ExpectationFailed(Error("Execution not found").toJson)
        case Some(ex) ⇒ success(ex)
      }
    }(r)

  def getId(success: ExecutionId ⇒ ActionResult)(implicit r: HttpServletRequest): ActionResult =
    Try(params("id")(r)) match {
      case Failure(_)  ⇒ ExpectationFailed(Error("id is missing").toJson)
      case Success(id) ⇒ success(ExecutionId(id))
    }

  def authenticate()(implicit r: HttpServletRequest) = {
    def fail = halt(401, Error("This service requires a valid token").toJson)

    Try(params("token")(r)) match {
      case Failure(_) ⇒ fail
      case Success(k) ⇒ if (!checkToken(k)) fail
    }
  }

}
