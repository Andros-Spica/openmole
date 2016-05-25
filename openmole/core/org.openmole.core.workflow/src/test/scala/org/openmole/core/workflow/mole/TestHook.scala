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
package org.openmole.core.workflow.mole

import monocle.macros.Lenses
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._

object TestHook {

  implicit def isBuilder = new HookBuilder[TestHook] {
    override def defaults = TestHook.defaults
    override def inputs = TestHook.inputs
    override def name = TestHook.name
    override def outputs = TestHook.outputs
  }

}

@Lenses case class TestHook(
    f:        Context ⇒ Context = identity[Context],
    inputs:   PrototypeSet      = PrototypeSet.empty,
    outputs:  PrototypeSet      = PrototypeSet.empty,
    defaults: DefaultSet        = DefaultSet.empty,
    name:     Option[String]    = None
) extends Hook {
  override protected def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider): Context =
    f(context)
}
