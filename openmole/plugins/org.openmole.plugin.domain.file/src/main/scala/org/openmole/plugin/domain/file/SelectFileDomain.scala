/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.domain.file

import java.io.File
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.tool.file.FileProvider

import scala.util.Random

object SelectFileDomain {

  implicit def isFinite = new Finite[File, SelectFileDomain] {
    override def computeValues(domain: SelectFileDomain, context: Context)(implicit rng: RandomProvider): Iterable[File] =
      domain.computeValues(context)

  }

  def apply(base: File, path: ExpandedString) = new SelectFileDomain(FileProvider(base, path))
}

class SelectFileDomain(val provider: FileProvider) {
  def computeValues(context: Context)(implicit rng: RandomProvider): Iterable[File] = List(provider(context))
}
