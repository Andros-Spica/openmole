/**
 * Created by Romain Reuillon on 22/04/16.
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
 *
 */
package org.openmole.core.workflow.validation

import org.openmole.core.context.Val
import org.openmole.core.tools.io.Prettifier
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._

trait ValidateTask {
  def validate: Seq[Throwable]
}

trait ValidateHook {
  def validate(inputs: Seq[Val[_]]): Seq[Throwable]
}

trait ValidateTransition {
  def validate(inputs: Seq[Val[_]]): Seq[Throwable]
}

object ValidationProblem {

  case class TaskValidationProblem(task: Task, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of task $task:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

  case class HookValidationProblem(hook: Hook, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of hook $hook:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

}

trait ValidationProblem extends Problem