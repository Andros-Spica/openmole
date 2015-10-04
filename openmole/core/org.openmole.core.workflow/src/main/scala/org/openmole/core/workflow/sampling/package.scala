/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._

package sampling {
  trait SamplingPackage {

    implicit def factorWithIterableToDiscreteFactor[T, D](f: Factor[T, D])(implicit discrete: Discrete[T, D]): DiscreteFactor[T, D] =
      DiscreteFactor(f)

    implicit def prototypeFactorDecorator[T](p: Prototype[T]) = new {
      def in[D](d: D)(implicit domain: Domain[T, D]): Factor[T, D] = Factor(p, d)(domain)
    }

    implicit def arrayPrototypeFactorDecorator[T: Manifest](p: Prototype[Array[T]]) = new {
      def is[D](d: D)(implicit discrete: Discrete[T, D]) = Factor(p, UnrolledDomain(d))
    }
  }
}

package object sampling extends SamplingPackage