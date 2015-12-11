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

import org.openmole.core.workflow.validation.Validation
import org.scalatest._
import org.openmole.core.dsl._

class WorkflowSpec extends FlatSpec with Matchers {

  def nsga2 = {
    val x = Val[Double]
    val y = Val[Double]

    val puzzle = EmptyTask() set (
      (inputs, outputs) += (x, y)
    )

    // Define a builder to use NSGA2 generational EA algorithm.
    // replicateModel is the fitness function to optimise.
    // lambda is the size of the offspring (and the parallelism level).
    SteadyStateEvolution(
      algorithm =
        NSGA2(
          mu = 100,
          genome = Genome(x in (0.0, 1.0), y in ("0.0", "1.0")),
          objectives = Seq(x, y),
          replication = Replication()
        ),
      evaluation = puzzle,
      parallelism = 10,
      termination = 10
    )
  }

  def conflict = {
    val population = Val[Double]
    val state = Val[Double]

    val puzzle = EmptyTask() set (
      (inputs, outputs) += (population, state)
    )

    // Define a builder to use NSGA2 generational EA algorithm.
    // replicateModel is the fitness function to optimise.
    // lambda is the size of the offspring (and the parallelism level).
    SteadyStateEvolution(
      algorithm =
        PSE(
          genome = Genome(population in (0.0, 1.0), state in ("0.0", "1.0")),
          gridSize = Seq(0.1, 0.1),
          objectives = Seq(population, state),
          replication = Replication()
        ),
      evaluation = puzzle,
      parallelism = 10,
      termination = 10
    )
  }

  "Steady state workflow" should "have no validation error" in {
    Validation(nsga2.toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(conflict.toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }

  "Island workflow" should "have no validation error" in {
    Validation(IslandEvolution(nsga2, 10, 50, 100).toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }

    Validation(IslandEvolution(conflict, 10, 50, 100).toMole).toList match {
      case Nil ⇒
      case l   ⇒ sys.error("Several validation errors have been found: " + l.mkString("\n"))
    }
  }
}
