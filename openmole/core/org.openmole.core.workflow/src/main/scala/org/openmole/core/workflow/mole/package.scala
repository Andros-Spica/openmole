/*
 * Copyright (C) 20/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow

import scala.language.implicitConversions

import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.execution.local._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

package mole {

  trait MolePackage

}

package object mole extends MolePackage {

  case class Hooks(map: Map[Capsule, Traversable[Hook]])

  case class Sources(map: Map[Capsule, Traversable[Source]])

  implicit def hooksToMap(h: Hooks): Map[Capsule, Traversable[Hook]] = h.map.withDefault(_ ⇒ List.empty)

  implicit def mapToHooks(m: Map[Capsule, Traversable[Hook]]): Hooks = new Hooks(m)

  implicit def iterableTupleToHooks(h: Iterable[(Capsule, Hook)]): Hooks =
    new Hooks(h.groupBy(_._1).mapValues(_.map(_._2)))

  implicit def sourcesToMap(s: Sources): Map[Capsule, Traversable[Source]] = s.map.withDefault(_ ⇒ List.empty)

  implicit def mapToSources(m: Map[Capsule, Traversable[Source]]): Sources = new Sources(m)

  implicit def iterableTupleToSources(s: Iterable[(Capsule, Source)]): Sources =
    new Sources(s.groupBy(_._1).mapValues(_.map(_._2)))

  object Hooks {
    def empty = Map.empty[Capsule, Traversable[Hook]]
  }

  object Sources {
    def empty = Map.empty[Capsule, Traversable[Source]]
  }

}