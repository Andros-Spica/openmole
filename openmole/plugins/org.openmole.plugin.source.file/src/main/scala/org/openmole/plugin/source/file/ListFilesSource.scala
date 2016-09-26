/*
 * Copyright (C) 11/06/13 Romain Reuillon
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

package org.openmole.plugin.source.file

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.dsl._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.mole._
import org.openmole.tool.random.RandomProvider
object ListFilesSource {

  implicit def isIO = InputOutputBuilder(ListFilesSource.config)

  def apply(path: FromContext[String], prototype: Val[Array[File]], regExp: FromContext[String] = ".*") =
    new ListFilesSource(
      path,
      prototype,
      regExp,
      config = InputOutputConfig()
    ) set (outputs += prototype)

}
@Lenses case class ListFilesSource(
    path:      FromContext[String],
    prototype: Val[Array[File]],
    regExp:    FromContext[String],
    config:    InputOutputConfig
) extends Source {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val expandedPath = new File(path.from(context))
    val expandedRegExp = regExp.from(context)
    Variable(
      prototype,
      expandedPath.listFilesSafe.filter(_.getName.matches(expandedRegExp))
    )
  }

}
