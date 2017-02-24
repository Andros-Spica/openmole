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

package org.openmole.plugin.method

import org.openmole.core.context._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation.DataflowProblem._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.plugin.tool.pattern._

package object directsampling {

  def Replication[T: Distribution](
    model:            Puzzle,
    seed:             Val[T],
    replications:     Int,
    distributionSeed: OptionalArgument[Long] = None,
    aggregation:      Puzzle                 = defaultAggregation
  ) =
    DirectSampling(
      model = model,
      sampling = seed in (TakeDomain(UniformDistribution[T](distributionSeed), replications)),
      aggregation = aggregation
    )

  def DirectSampling(
    model:       Puzzle,
    sampling:    Sampling,
    aggregation: Puzzle   = defaultAggregation
  )(): Puzzle = {
    val explorationSkel = ExplorationTask(sampling) set (
      name := "exploration"
    )

    val missing =
      Validation(explorationSkel -< model).collect {
        case MissingInput(_, d) ⇒ d
      }

    val exploration = explorationSkel set ((inputs, outputs) += (missing: _*))
    val explorationCapsule = StrainerCapsule(exploration)

    Strain(explorationCapsule -< model >- aggregation)
  }

  private def defaultAggregation =
    Strain(EmptyTask() set (name := "aggregation"))

}
