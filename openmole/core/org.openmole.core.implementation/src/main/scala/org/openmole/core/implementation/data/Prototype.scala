/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.tools.obj.Id
import scala.reflect.Manifest

object Prototype {
  
  def toArray[T](prototype: IPrototype[T], nbLevel: Int): IPrototype[_] = {
    if(nbLevel <= 0) prototype
    else { 
      val arrayProto = new Prototype(prototype.name, prototype.`type`.arrayManifest).asInstanceOf[IPrototype[Array[_]]]
      if(nbLevel <= 1) arrayProto
      else toArray(arrayProto, nbLevel - 1)
    }
  }

  def fromArray[T](prototype: IPrototype[Array[T]]): IPrototype[T] =
    new Prototype(prototype.name, prototype.`type`.fromArray).asInstanceOf[IPrototype[T]]
  
  def toArray[T](prototype: IPrototype[T]): IPrototype[Array[T]] =
    new Prototype(prototype.name, prototype.`type`.arrayManifest).asInstanceOf[IPrototype[Array[T]]]

  implicit lazy val prototypeOrderingOnName = new Ordering[IPrototype[_]] {
    override def compare(left: IPrototype[_], right: IPrototype[_]) = 
      left.name compare right.name
  }
  
}

class Prototype[T](val name: String, val `type`: Manifest[T]) extends IPrototype[T] with Id {
 
  import Prototype._
  
  def this(name: String, clazz: Class[T]) = this(name, clazz.equivalence.toManifest)
  
  override def isAssignableFrom(p: IPrototype[_]): Boolean = 
    `type`.isAssignableFromHighOrder(p.`type`)

  override def accepts(obj: Any): Boolean =
    obj == null || `type`.isAssignableFromHighOrder(manifest(clazzOf(obj)))
  
  override def id = (name, `type`)
  override def toString = name + ": " + `type`.toString
}
