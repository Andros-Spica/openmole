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
package org.openmole.plugin.tool.pattern

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.Condition
import org.openmole.core.workflow.transition._

object Case {
  def apply(puzzle: Puzzle): Case = Case(Condition.True, puzzle)
}

case class Case(condition: Condition, puzzle: Puzzle)

object Switch {

  def apply(cases: Case*) = {
    val first = Capsule(EmptyTask(), strain = true)
    val firstSlot = Slot(first)
    val last = Capsule(EmptyTask(), strain = true)

    val alternatives: Seq[Puzzle] =
      cases.map {
        case Case(condition, puzzle) ⇒
          firstSlot -- (puzzle when condition) -- last
      }

    Puzzle.merge(firstSlot, Seq(last), puzzles = alternatives)
  }

}
