/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.plugin.task.template

import java.io.File

import org.openmole.core.context.Prototype
import org.openmole.core.expansion.ExpandedString
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object TemplateFileFromInputTask {

  def apply(
    template: Prototype[File],
    output:   Prototype[File]
  ) =
    ClosureTask("TemplateFileFromInputTask") { (context, rng, executionContext) ⇒
      implicit val impRng = rng
      val expanded = context(template).withInputStream { is ⇒ ExpandedString(is).from(context) }
      val file = executionContext.tmpDirectory.newFile("template", ".tmp")
      file.content = expanded
      context + (output → file)
    } set (
      inputs += template,
      outputs += output
    )
}
