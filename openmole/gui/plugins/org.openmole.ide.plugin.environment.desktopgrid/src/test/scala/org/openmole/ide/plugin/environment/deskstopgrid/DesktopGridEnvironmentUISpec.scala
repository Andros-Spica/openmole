/*
 * Copyright (C) 2013 Mathieu Leclaire
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

package org.openmole.ide.plugin.environment.desktopgrid

import org.openmole.ide.core.implementation.serializer.GUISerializer
import org.scalatest.FlatSpec
import org.scalatest._

class DesktopEnvironmentUISpec extends FlatSpec with Matchers {

  "DesktopEnvironmentDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("09.xml")) should equal(true)
  }
}
