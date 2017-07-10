/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.batch.environment

import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

import org.openmole.core.communication.message._
import org.openmole.core.console.ScalaREPL.ReferencedClasses
import org.openmole.core.console.{ REPLClassloader, ScalaREPL }
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.fileservice.{ FileCache, FileService }
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.{ ThreadProvider, Updater }
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.cache._
import org.openmole.tool.file._
import org.openmole.tool.logger.Logger
import org.openmole.tool.random.{ RandomProvider, Seeder }
import squants.time.TimeConversions._
import squants.information.Information
import squants.information.InformationConversions._
import org.openmole.core.location._

import scala.ref.WeakReference
import scala.util.Random

object BatchEnvironment extends Logger {

  trait Transfer {
    def id: Long
  }

  case class BeginUpload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndUpload(id: Long, file: File, path: String, storage: StorageService, exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = exception.isEmpty
  }

  case class BeginDownload(id: Long, file: File, path: String, storage: StorageService) extends Event[BatchEnvironment] with Transfer
  case class EndDownload(id: Long, file: File, path: String, storage: StorageService, exception: Option[Throwable]) extends Event[BatchEnvironment] with Transfer {
    def success = exception.isEmpty
  }

  def signalUpload[T](id: Long, upload: ⇒ T, file: File, path: String, storage: StorageService)(implicit eventDispatcher: EventDispatcher): T = {
    eventDispatcher.trigger(storage.environment, BeginUpload(id, file, path, storage))
    val res =
      try upload
      catch {
        case e: Throwable ⇒
          eventDispatcher.trigger(storage.environment, EndUpload(id, file, path, storage, Some(e)))
          throw e
      }
    eventDispatcher.trigger(storage.environment, EndUpload(id, file, path, storage, None))
    res
  }

  def signalDownload[T](id: Long, download: ⇒ T, path: String, storage: StorageService, file: File)(implicit eventDispatcher: EventDispatcher): T = {
    eventDispatcher.trigger(storage.environment, BeginDownload(id, file, path, storage))
    val res =
      try download
      catch {
        case e: Throwable ⇒
          eventDispatcher.trigger(storage.environment, EndDownload(id, file, path, storage, Some(e)))
          throw e
      }
    eventDispatcher.trigger(storage.environment, EndDownload(id, file, path, storage, None))
    res
  }

  val MemorySizeForRuntime = ConfigurationLocation("BatchEnvironment", "MemorySizeForRuntime", Some(1024 megabytes))

  val CheckInterval = ConfigurationLocation("BatchEnvironment", "CheckInterval", Some(1 minutes))

  val GetTokenInterval = ConfigurationLocation("BatchEnvironment", "GetTokenInterval", Some(1 minutes))

  val MinUpdateInterval = ConfigurationLocation("BatchEnvironment", "MinUpdateInterval", Some(1 minutes))
  val MaxUpdateInterval = ConfigurationLocation("BatchEnvironment", "MaxUpdateInterval", Some(10 minutes))
  val IncrementUpdateInterval = ConfigurationLocation("BatchEnvironment", "IncrementUpdateInterval", Some(1 minutes))
  val MaxUpdateErrorsInARow = ConfigurationLocation("BatchEnvironment", "MaxUpdateErrorsInARow", Some(3))
  val StoragesGCUpdateInterval = ConfigurationLocation("BatchEnvironment", "StoragesGCUpdateInterval", Some(1 hours))
  val RuntimeMemoryMargin = ConfigurationLocation("BatchEnvironment", "RuntimeMemoryMargin", Some(400 megabytes))

  val downloadResultRetry = ConfigurationLocation("BatchEnvironment", "DownloadResultRetry", Some(3))

  private def runtimeDirLocation = openMOLELocation / "runtime"

  lazy val runtimeLocation = runtimeDirLocation / "runtime.tar.gz"
  lazy val JVMLinuxX64Location = runtimeDirLocation / "jvm-x64.tar.gz"

  def defaultRuntimeMemory(implicit preference: Preference) = preference(BatchEnvironment.MemorySizeForRuntime)
  def getTokenInterval(implicit preference: Preference, randomProvider: RandomProvider) = preference(GetTokenInterval) * randomProvider().nextDouble

  def openMOLEMemoryValue(openMOLEMemory: Option[Information])(implicit preference: Preference) = openMOLEMemory match {
    case None    ⇒ preference(MemorySizeForRuntime)
    case Some(m) ⇒ m
  }

  def requiredMemory(openMOLEMemory: Option[Information], memory: Option[Information])(implicit preference: Preference) = memory match {
    case Some(m) ⇒ m
    case None    ⇒ openMOLEMemoryValue(openMOLEMemory) + preference(BatchEnvironment.RuntimeMemoryMargin)
  }

  def threadsValue(threads: Option[Int]) = threads.getOrElse(1)

  object Services {

    implicit def fromServices(implicit services: org.openmole.core.services.Services): Services = {
      import services._
      new Services()
    }
  }

  class Services(
    implicit
    val threadProvider:             ThreadProvider,
    implicit val preference:        Preference,
    implicit val newFile:           NewFile,
    implicit val serializerService: SerializerService,
    implicit val fileService:       FileService,
    implicit val seeder:            Seeder,
    implicit val randomProvider:    RandomProvider,
    implicit val replicaCatalog:    ReplicaCatalog,
    implicit val eventDispatcher:   EventDispatcher
  )

}

trait BatchEnvironment extends SubmissionEnvironment { env ⇒
  type SS <: StorageService
  type JS <: JobService

  implicit val services: BatchEnvironment.Services
  implicit def preference = services.preference
  implicit def eventDispatcher = services.eventDispatcher

  def executionJob(job: Job): BatchExecutionJob

  lazy val batchJobWatcher = new BatchJobWatcher(WeakReference(this), preference)
  def jobs = batchJobWatcher.executionJobs

  def threads: Option[Int] = None
  def openMOLEMemory: Option[Information]

  override def submit(job: Job) = {
    val bej = executionJob(job)
    batchJobWatcher.register(bej)
    eventDispatcher.trigger(this, new Environment.JobSubmitted(bej))
    JobManager ! Manage(bej)
  }

  def runtime = BatchEnvironment.runtimeLocation
  def jvmLinuxX64 = BatchEnvironment.JVMLinuxX64Location

  @transient val plugins = Cache { PluginManager.pluginsForClass(this.getClass) }

  def updateInterval =
    UpdateInterval(
      minUpdateInterval = services.preference(BatchEnvironment.MinUpdateInterval),
      maxUpdateInterval = services.preference(BatchEnvironment.MaxUpdateInterval),
      incrementUpdateInterval = services.preference(BatchEnvironment.IncrementUpdateInterval)
    )

  def submitted: Long = jobs.count { _.state == ExecutionState.SUBMITTED }
  def running: Long = jobs.count { _.state == ExecutionState.RUNNING }

  def runtimeSettings = RuntimeSettings(archiveResult = false)

  override def start() = {
    super.start()
    import services.threadProvider
    Updater.registerForUpdate(batchJobWatcher)
  }

  override def stop() = {
    super.stop()
    jobs.foreach(ej ⇒ JobManager ! Kill(ej))
    batchJobWatcher.stop = true
  }

}

class SimpleBatchExecutionJob(val job: Job, val environment: SimpleBatchEnvironment) extends ExecutionJob with BatchExecutionJob { bej ⇒

  def trySelectStorage(files: ⇒ Vector[File]) = {
    val s = environment.storage
    s.tryGetToken.map(t ⇒ (s, t))
  }
  def trySelectJobService() = {
    val js = environment.jobService
    js.tryGetToken.map(t ⇒ (js, t))
  }

}

trait SimpleBatchEnvironment <: BatchEnvironment { env ⇒
  type BEJ = SimpleBatchExecutionJob

  def executionJob(job: Job): BEJ = new SimpleBatchExecutionJob(job, this)

  def storage: SS
  def jobService: JS
}

object BatchExecutionJob {
  val replBundleCache = new AssociativeCache[ReferencedClasses, FileCache]()
}

trait BatchExecutionJob extends ExecutionJob { bej ⇒

  def job: Job
  var serializedJob: Option[SerializedJob] = None
  var batchJob: Option[BatchJob] = None

  def moleJobs = job.moleJobs
  def runnableTasks = job.moleJobs.map(RunnableTask(_))

  def plugins = pluginsAndFiles.plugins ++ closureBundle.map(_.file) ++ referencedClosures.toSeq.flatMap(_.plugins)
  def files = pluginsAndFiles.files

  @transient lazy val pluginsAndFiles = environment.services.serializerService.pluginsAndFiles(runnableTasks)

  @transient lazy val referencedClosures = {
    if (pluginsAndFiles.replClasses.isEmpty) None
    else {
      def referenced =
        pluginsAndFiles.replClasses.map { c ⇒
          val replClassloader = c.getClassLoader.asInstanceOf[REPLClassloader]
          replClassloader.referencedClasses(Seq(c))
        }.fold(ReferencedClasses.empty)(ReferencedClasses.merge)
      Some(referenced)
    }
  }

  def closureBundle =
    referencedClosures.map { closures ⇒
      BatchExecutionJob.replBundleCache.cache(job.moleExecution, closures, preCompute = false) { rc ⇒
        val bundle = environment.services.newFile.newFile("closureBundle", ".jar")
        try ScalaREPL.bundleFromReferencedClass(closures, "closure-" + UUID.randomUUID.toString, "1.0", bundle)
        catch {
          case e: Throwable ⇒
            bundle.delete()
            throw e
        }
        FileCache(bundle)(environment.services.fileService)
      }
    }

  def usedFiles: Iterable[File] =
    (files ++
      Seq(environment.runtime, environment.jvmLinuxX64) ++
      environment.plugins() ++ plugins).distinct

  def usedFileHashes = usedFiles.map(f ⇒ (f, environment.services.fileService.hash(f)(environment.services.newFile)))

  def environment: BatchEnvironment

  def trySelectStorage(files: ⇒ Vector[File]): Option[(StorageService, AccessToken)]
  def trySelectJobService(): Option[(JobService, AccessToken)]

}