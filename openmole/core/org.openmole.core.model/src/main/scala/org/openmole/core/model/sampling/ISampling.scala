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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.sampling

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable

trait ISampling {
  /**
   * This method builds the explored plan in the givern {@code context}.
   *
   * @param context context in which the exploration takes place
   */
  @throws(classOf[Throwable])
  def build(context: IContext): Iterable[Iterable[IVariable[_]]]

}
