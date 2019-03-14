/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.builder._
import org.scalatest._
import org.scalatest.junit._

import scala.collection.mutable.ListBuffer
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.test.TestTask
import org.openmole.tool.random.RandomProvider

class MoleExecutionSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.test.Stubs._

  class JobGroupingBy2Test extends Grouping {

    def apply(context: Context, groups: Iterable[(MoleJobGroup, Iterable[MoleJob])])(implicit newGroup: NewGroup, randomProvider: RandomProvider): MoleJobGroup = {
      groups.find { case (_, g) ⇒ g.size < 2 } match {
        case Some((mg, _)) ⇒ mg
        case None          ⇒ MoleJobGroup()
      }
    }

  }

  "Grouping jobs" should "not impact a normal mole execution" in {
    val data = List("A", "A", "B", "C")
    val i = Val[String]("i")

    val sampling = ExplicitSampling(i, data)
    val emptyT = EmptyTask() set ((inputs, outputs) += i)

    val testT = TestTask { context ⇒
      context.contains(i.toArray) should equal(true)
      context(i.toArray).sorted.deep should equal(data.toArray.deep)
      context
    } set (inputs += i.array)

    val ex = ExplorationTask(sampling) -< (emptyT by new JobGroupingBy2Test) >- testT

    ex.run
  }

  "Implicits" should "be used when input is missing" in {
    val i = Val[String]("i")
    val emptyT = EmptyTask() set (inputs += i)
    val emptyC = MoleCapsule(emptyT)
    MoleExecution(mole = Mole(emptyC), implicits = Context(Variable(i, "test"))).run
  }

  "Wait" should "wait for the mole executon to be completed" in {
    val emptyT = EmptyTask()
    val me = emptyT.start()
    me.hangOn()
  }
}
