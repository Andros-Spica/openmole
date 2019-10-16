/**
 * Created by Romain Reuillon on 20/01/16.
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
 *
 */
package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object InitialStateTask {

  def apply(evolution: EvolutionWorkflow)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("InitialStateTask") { (context, _, _) ⇒
      def initialisedState =
        evolution.operations.startTimeLens.set(System.currentTimeMillis) andThen
          evolution.operations.generationLens.set(0L) apply context(evolution.statePrototype)

      Context(Variable(evolution.statePrototype, initialisedState))
    } set (
      inputs += (evolution.statePrototype, evolution.populationPrototype),
      outputs += (evolution.statePrototype, evolution.populationPrototype),
      evolution.statePrototype := FromContext(p ⇒ evolution.operations.initialState),
      evolution.populationPrototype := Array.empty[evolution.I](evolution.integration.iManifest)
    )

}
