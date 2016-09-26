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
package org.openmole.core.expansion

import org.openmole.core.tools.service
import org.openmole.core.workspace.Workspace
import org.openmole.tool.file.FilePackage
import org.openmole.tool.random
import org.openmole.tool.statistics.StatisticsPackage

trait CodePackage extends FilePackage with StatisticsPackage {
  def Random(seed: Long) = random.Random.apply(seed)
  def newRNG(seed: Long) = Random(seed)

  def newFile(prefix: String = Workspace.fixedPrefix, suffix: String = Workspace.fixedPostfix) = Workspace.newFile(prefix, suffix)
  def newDir(prefix: String = Workspace.fixedDir) = Workspace.newDir(prefix)
  def mkDir(prefix: String = Workspace.fixedDir) = {
    val dir = Workspace.newDir(prefix)
    dir.mkdirs
    dir
  }
}

object CodePackage extends CodePackage {
  def namespace = s"${this.getClass.getPackage.getName}.${classOf[CodePackage].getSimpleName}"
}