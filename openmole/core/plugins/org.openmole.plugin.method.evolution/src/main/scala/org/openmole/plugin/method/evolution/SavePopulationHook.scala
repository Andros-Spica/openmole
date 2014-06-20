/*
 * Copyright (C) 2014 Romain Reuillon
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

import ga._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.plugin.hook.file.AppendToCSVFileHook

object SavePopulationHook {

  def apply(puzzle: GAPuzzle[GAAlgorithm], dir: String): AppendToCSVFileHook.Builder = apply(puzzle, dir, "/population${" + puzzle.generation.name + "}.csv")

  def apply(puzzle: GAPuzzle[GAAlgorithm], dir: String, name: String): AppendToCSVFileHook.Builder = {
    import puzzle._
    val builder = new AppendToCSVFileHook.Builder(dir + "/" + name)
    builder.add(puzzle.generation)
    evolution.inputs.inputs.foreach(i ⇒ builder.add(i.prototype.toArray))
    evolution.objectives.foreach { case (o, _) ⇒ builder.add(o.toArray) }
    builder
  }

}
