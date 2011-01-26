/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.task

import org.openmole.commons.aspect.eventdispatcher.IObjectListenerWithArgs
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.MultipleException
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.service.Priority
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.internal.Activator._
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.mole.MoleJobRegistry
import org.openmole.core.implementation.tools.ContextAggregator
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IMoleTask
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

class MoleTask(name: String, val mole: IMole) extends Task(name) with IMoleTask {

  class ResultGathering extends IObjectListenerWithArgs[IMoleExecution] {

    val throwables = new ListBuffer[Throwable] 
    val contexts = new ListBuffer[IContext] 
    
    override def eventOccured(t: IMoleExecution, os: Array[Object]) = synchronized {
      val moleJob = os(0).asInstanceOf[IMoleJob]

      moleJob.state match {
        case State.FAILED =>
           throwables += moleJob.context.value(GenericTask.Exception.prototype).getOrElse(new InternalProcessingError("BUG: Job has failed but no exception can be found"))
        case State.COMPLETED =>
           MoleJobRegistry(moleJob).foreach{ 
             e => outputCapsules.get(e._2).foreach {
               prototypes => 
                val ctx = new Context
                for(p <- prototypes) moleJob.context.variable(p).foreach{ctx += _}
                contexts += ctx
             }
           }
        case _ =>
      }
    }
  }
   

  private val outputCapsules = new HashMap[IGenericCapsule, ListBuffer[IPrototype[_]]]

  override protected def process(context: IContext, progress: IProgress) = {
    val firstTaskContext = new Context

    for (input <- inputs) {
      if (!input.mode.isOptional || (input.mode.isOptional && context.contains(input.prototype))) {
        firstTaskContext += context.variable(input.prototype).getOrElse(throw new InternalProcessingError("Bug: variable not found."))        
      }
    }

    val execution = new MoleExecution(mole)

    val resultGathering = new ResultGathering
    eventDispatcher.registerForObjectChangedSynchronous(execution, Priority.NORMAL, resultGathering, IMoleExecution.OneJobFinished);

    execution.start(firstTaskContext)
    execution.waitUntilEnded

    ContextAggregator.aggregate(userOutputs, false, resultGathering.contexts).foreach {
      context += _
    }

    val exceptions = resultGathering.throwables

    if (!exceptions.isEmpty) {
      context += (GenericTask.Exception.prototype, new MultipleException(exceptions))
    }
  }

  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_]): Unit = {
    addOutput(capsule, new Data(prototype))
  }

  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_],masks: Array[DataModeMask]): Unit = {
    addOutput(capsule, new Data(prototype, masks))
  }

  def addOutput(capsule: IGenericCapsule, data: IData[_]): Unit = {
    addOutput(data)
    outputCapsules.getOrElseUpdate(capsule, new ListBuffer[IPrototype[_]]) += data.prototype
  }
  
  override def inputs: IDataSet = {
    val firstTask = mole.root.task match {
      case None => throw new UserBadDataError("First task has not been assigned in the mole of the mole task " + name)
      case Some(t) => t
    }
    new DataSet(super.inputs ++ firstTask.inputs)
  }
}
