package org.openmole.site

import scalatags.Text.{ TypedTag, tags2 }
import scalatags.Text.all._
import org.openmole.site.tools._

/*
 * Copyright (C) 28/06/17 // mathieu.leclaire@openmole.org
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

object Footer {

  val WHITE = color := "#e6e6e6"

  val titleStyle = Seq(
    fontSize := "15px",
    marginLeft := 10
  )

  val liStyle = Seq(
    paddingLeft := 50,
    paddingBottom := 25,
    paddingTop := 25
  )

  def subItem(i: TypedTag[String]) = div(paddingTop := 15)(i)

  def imgSubItem(image: FileResource, title: String, link: String, text: String = "", otherTab: Boolean = true) =
    subItem(
      div(width := 200)(
        tools.to(link, otherTab = otherTab)(
          img(src := image.file, height := 25, paddingBottom := 5)(span(s"$title", titleStyle))
        ),
        div(text, fontSize := "12px")
      )
    )

  val build = {
    div(
      tags2.nav(classIs(navbar_default ++ navbar_inverse), stylesheet.footer)(
        div(classIs(container_fluid), stylesheet.center(50))(
          div(classIs(collapse ++ navbar_collapse))(
            ul(classIs(nav ++ navbar_nav))(
              li(span("COMMUNITY", WHITE)(
                div(paddingTop := 15),
                imgSubItem(Resource.img.github, "Source Repository", shared.link.repo.openmole, "Follow the commits, submit an issue or take part to the devloppement !"),
                imgSubItem(Resource.img.previousVersion, "Previous versions", Pages.previousVersions.file, "Downloads and change logs of previous versions", false),
                imgSubItem(Resource.img.email, "Forum", shared.link.mailingList, "Both forum and mailing list(subscribe first)"),
                imgSubItem(Resource.img.faq, "FAQ", Pages.faq.file, "Any questions you may have", false)
              ), liStyle),
              li(span("ABOUT US", WHITE)(
                div(paddingTop := 15),
                imgSubItem(Resource.img.paper, "Publications", Pages.communications.file, "Papers referencing OpenMOLE. How to cite us.", true),
                imgSubItem(Resource.img.whoarwe, "Who are we ?", Pages.whoAreWe.file, "Developpment team, partners", false),
                imgSubItem(Resource.img.partner, "Partners", Pages.partner.file, "Developpment team, partners", false)
              ), liStyle),
              li(span("COMMUNICATION", WHITE)(
                div(paddingTop := 15),
                imgSubItem(Resource.img.blog, "Blog", shared.link.blog, "Nice stories about OpenMOLE"),
                imgSubItem(Resource.img.twitter, "Twitter", shared.link.twitter, "#openmole #model #optimization #hpc #amazing")
              ), liStyle)
            )
          )
        )
      )
    )
  }

}
