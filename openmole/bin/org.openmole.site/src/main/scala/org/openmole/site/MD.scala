/**
 * Created by Romain Reuillon on 05/07/16.
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
package org.openmole.site

import java.lang.StringBuilder
import java.util

import com.github.rjeschke._
import com.github.rjeschke.txtmark.DefaultDecorator
import org.apache.commons.lang3.StringEscapeUtils
import org.openmole.site.market.GeneratedMarketEntry
import org.openmole.tool.file._

import scala.collection.JavaConversions._
import scala.xml.XML
import scala.xml.parsing.XhtmlParser
import scalatags.Text.all._
import scalaz._

object MD {

  val emiter = new txtmark.BlockEmitter {
    override def emitBlock(stringBuilder: StringBuilder, list: util.List[String], s: String): Unit = {
      def code = list.mkString("\n")
      val html =
        if (s == "openmole") hl.openmole(code)
        else hl.highlight(code, s)
      stringBuilder.append(html.render)
    }
  }

  def mdToHTML(md: String) = {
    val configuration =
      txtmark.Configuration.builder().
        setCodeBlockEmitter(emiter).
        forceExtentedProfile().build()
    txtmark.Processor.process(md, configuration)
  }

  def prefixLink(prefix: String)(n: Seq[scala.xml.Node]) = {
    import scala.xml.transform._

    val HTMLTransformer = new RuleTransformer(new RewriteRule {
      override def transform(node: scala.xml.Node) =
        node match {
          case image @ <img/> ⇒
            val newSrc = prefix + image \ "@src"
            val alt = image \ "@alt"
            <img src={ newSrc } style="max-width:100%;" alt={ alt }/>
          case link @ <a>{ stuff }</a> ⇒
            val uri = new java.net.URI(link \ "@href" text)
            if (!uri.isAbsolute) {
              val newTarget = prefix + link \ "@href"
              <a href={ newTarget }>
                { stuff }
              </a>
            }
            else link
          case passthrough ⇒ passthrough
        }
    })

    HTMLTransformer.transform(n)
  }

  def relativiseLinks(md: String, prefix: String) = {
    def escaped = s"<div>${StringEscapeUtils.unescapeHtml4(md)}</div>"
    prefixLink(prefix)(XML.loadString(escaped)).mkString
  }

  def generatePage(entry: GeneratedMarketEntry)(implicit parent: Parent[DocumentationPage]) =
    entry.readme.map { md ⇒
      def frag = RawFrag(relativiseLinks(MD.mdToHTML(md), entry.entry.name + "/"))
      DocumentationPages(entry.entry.name, Reader(_ ⇒ frag), location = Some(Seq(entry.entry.name, entry.entry.name)))
    }

}
