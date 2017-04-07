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

package org.openmole.core.workflow.task

import java.util.concurrent.locks.ReentrantLock

import monocle.macros.Lenses
import org.openmole.core.context._
import org.openmole.core.event._
import org.openmole.core.exception._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.lock._
import org.openmole.tool.random.Seeder

object MoleTask {

  implicit def isTask = InputOutputBuilder(MoleTask.config)

  def apply(puzzle: Puzzle)(implicit name: sourcecode.Name): MoleTask =
    apply(puzzle toMole, puzzle.lasts.head)

  /**
   * *
   * @param mole the mole executed by this task.
   * @param last the capsule which returns the results
   */
  def apply(mole: Mole, last: Capsule)(implicit name: sourcecode.Name): MoleTask = {
    val mt = new MoleTask(_mole = mole, last = last)

    mt set (
      dsl.inputs += (mole.root.inputs(mole, Sources.empty, Hooks.empty).toSeq: _*),
      dsl.outputs += (last.outputs(mole, Sources.empty, Hooks.empty).toSeq: _*),
      isTask.defaults.set(mole.root.task.defaults)
    )
  }

}

@Lenses case class MoleTask(
    _mole:     Mole,
    last:      Capsule,
    implicits: Vector[String]    = Vector.empty,
    config:    InputOutputConfig = InputOutputConfig()
) extends Task {

  def mole = _mole.copy(inputs = inputs)

  protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { p ⇒
    import p._
    val implicitsValues = implicits.flatMap(i ⇒ context.get(i))
    implicit val seeder = Seeder(random().nextLong())
    implicit val eventDispatcher = EventDispatcher()
    implicit val newFile = NewFile(executionContext.tmpDirectory.newDir("moletask"))

    import executionContext.preference
    import executionContext.threadProvider
    import executionContext.workspace

    val execution =
      MoleExecution(
        mole,
        implicits = implicitsValues,
        defaultEnvironment = executionContext.localEnvironment,
        executionContext = MoleExecutionContext(),
        cleanOnFinish = false
      )

    @volatile var lastContext: Option[Context] = None
    val lastContextLock = new ReentrantLock()

    execution listen {
      case (_, ev: MoleExecution.JobFinished) ⇒
        lastContextLock { if (ev.capsule == last) lastContext = Some(ev.moleJob.context) }
    }

    execution.start(context)
    try execution.waitUntilEnded
    catch {
      case e: ThreadDeath ⇒
        execution.cancel
        throw e
      case e: InterruptedException ⇒
        execution.cancel
        throw e
    }

    lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

}
