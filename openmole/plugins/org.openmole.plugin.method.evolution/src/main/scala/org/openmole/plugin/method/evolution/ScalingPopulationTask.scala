/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object ScalingPopulationTask {

  def apply[T](algorithm: T)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    ClosureTask("ScalingPopulationTask") { (context, rng, _) ⇒
      t.populationToVariables(context(t.populationPrototype)).from(context)(rng)
    } set (
      inputs += t.populationPrototype,
      outputs += (t.resultPrototypes.map(_.array): _*)
    )
  }
}
