/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.method.evolution

import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import monocle.macros.Lenses
import org.openmole.core.dsl._

object ReassignStateRNGTask {

  def apply[T](algorithm: T)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    ClosureTask("ReassignStateRNGTask") { (context, _, _) ⇒
      Context(Variable(t.statePrototype, t.operations.randomLens.set(Task.buildRNG(context))(context(t.statePrototype))))
    } set (
      (inputs, outputs) += t.statePrototype
    )
  }

}
