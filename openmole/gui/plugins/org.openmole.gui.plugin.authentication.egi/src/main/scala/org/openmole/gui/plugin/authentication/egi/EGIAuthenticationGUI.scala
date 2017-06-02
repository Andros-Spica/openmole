/**
 * Created by Romain Reuillon on 28/11/16.
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
package org.openmole.gui.plugin.authentication.egi

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory, Test }
import org.openmole.gui.ext.tool.client.{ FileUploaderUI, OMPost }
import org.openmole.gui.ext.tool.client.JsRxTags._
import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.{ all ⇒ sheet }
import autowire._
import sheet._
import bs._
import org.openmole.gui.ext.api.Api
import org.scalajs.dom.raw.HTMLInputElement
import rx._

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.all._

@JSExport
class EGIAuthenticationGUIFactory extends AuthenticationPluginFactory {
  type AuthType = EGIAuthenticationData

  def buildEmpty: AuthenticationPlugin = new EGIAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new EGIAuthenticationGUI(data)

  def name = "EGI"

  def getData: Future[Seq[AuthType]] = OMPost()[EGIAuthenticationAPI].egiAuthentications().call()
}

@JSExport
class EGIAuthenticationGUI(val data: EGIAuthenticationData = EGIAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = EGIAuthenticationData

  val passwordStyle: ModifierSeq = Seq(
    width := 130,
    passwordType
  )

  val password = bs.input(data.cypheredPassword)(placeholder := "Password", passwordStyle).render
  val privateKey = FileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined, Some("egi.p12"))

  val voInput = bs.input("")(placeholder := "vo1,vo2").render

  OMPost()[EGIAuthenticationAPI].geVOTest().call().foreach {
    _.foreach { c ⇒
      voInput.value = c
    }
  }

  def factory = new EGIAuthenticationGUIFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[EGIAuthenticationAPI].removeAuthentication().call().foreach { _ ⇒
    onremove()
  }

  lazy val panel = vForm(
    password.withLabel("Password"),
    privateKey.view(sheet.marginTop(10)).render.withLabel("Certificate"),
    voInput.withLabel("Test EGI credential on", sheet.paddingTop(40))
  )

  def save(onsave: () ⇒ Unit) = {
    OMPost()[EGIAuthenticationAPI].removeAuthentication().call().foreach {
      d ⇒
        OMPost()[EGIAuthenticationAPI].addAuthentication(EGIAuthenticationData(
          cypheredPassword = password.value,
          privateKey = if (privateKey.pathSet.now) Some("egi.p12") else None
        )).call().foreach {
          b ⇒
            onsave()
        }
    }

    OMPost()[EGIAuthenticationAPI].setVOTest(voInput.value.split(",").map(_.trim).toSeq).call()
  }

  def test = OMPost()[EGIAuthenticationAPI].testAuthentication(data).call()

}
