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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.transition._

object Strain {

  def apply(puzzle: Puzzle) = {
    val first = Capsule(EmptyTask(), strainer = true)
    val last = Slot(Capsule(EmptyTask(), strainer = true))

    val mole = puzzle.toMole
    val sources = Sources(puzzle.sources.groupBy(_._1).mapValues(_.map(_._2)))
    val hooks = Hooks(puzzle.hooks.groupBy(_._1).mapValues(_.map(_._2)))
    val outputs = puzzle.lasts.foldLeft(PrototypeSet.empty) {
      case (union, capsule) ⇒ union ++ capsule.outputs(mole, sources, hooks)
    }.toSeq

    (first -- puzzle -- last) + (first -- (last, filter = Block(outputs.map(_.name): _*)))
  }

}