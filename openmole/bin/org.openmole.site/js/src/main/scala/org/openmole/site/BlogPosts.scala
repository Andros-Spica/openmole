package org.openmole.site

import org.scalajs.dom._
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLElement }

import scala.util.{ Failure, Success }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalatags.JsDom.all._
import rx._

import scalatags.Text.RawFrag
import scalatags.Text.all.{ backgroundColor, padding }
import scaladget.api.{ BootstrapTags ⇒ bs }
import bs._
import scalatags.JsDom.TypedTag

/*
 * Copyright (C) 13/07/17 // mathieu.leclaire@openmole.org
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

object BlogPosts {

  type Category = String

  val titleTag = "title"
  val linkTag = "link"
  val dateTag = "pubDate"
  val categoryTag = "category"
  val searchedTags = Seq(titleTag, linkTag, dateTag, categoryTag)

  val newsCategory = "News"
  val shortTrainingCategory = "ShortTraining"
  val longTrainingCategory = "LongTraining"

  case class ReadNode(index: Int, name: String, value: String)
  case class BlogPost(title: String = "", category: Category = newsCategory, link: String = "", date: String = "")

  val all: Var[Seq[BlogPost]] = Var(Seq())
  private def allBy(category: Category) = all.now.filter { _.category == category }.take(3)

  all.trigger {
    if (!all.now.isEmpty) {
      addNewsdiv(allBy(newsCategory))
      // addTrainings(allBy(shortTrainingCategory), shared.shortTraining)
      // addTrainings(allBy(longTrainingCategory), shared.longTraining)
    }
  }

  def fetch = {
    val blogPosts = org.scalajs.dom.window.sessionStorage.getItem(shared.blogposts)
    if (blogPosts == null) {
      val future = ext.Ajax.get(
        headers = Map(
          "Accept" → "*/*"
        ),
        url = s"${shared.link.blog}/rss/",
        timeout = 10000
      ).map {
          _.responseText
        }

      future.onComplete {
        case Failure(f) ⇒ f.getStackTrace.mkString(" ")
        case Success(s) ⇒
          org.scalajs.dom.window.sessionStorage.setItem(shared.blogposts, s)
          all() = s
      }
    }
    else
      all() = blogPosts
  }

  implicit def readNodeToBlogPost(rns: Seq[ReadNode]): BlogPost = {
    def toBB(rs: Seq[ReadNode], blogPost: BlogPost): BlogPost = {
      if (rs.isEmpty) blogPost
      else toBB(rs.tail, {
        val head = rs.head
        val value = head.value
        head.name match {
          case n if n.startsWith(titleTag)    ⇒ blogPost.copy(title = value)
          case n if n.startsWith(categoryTag) ⇒ blogPost.copy(category = value)
          case n if n.startsWith(linkTag)     ⇒ blogPost.copy(link = value)
          case _                              ⇒ blogPost
        }
      })
    }
    toBB(rns, BlogPost())
  }

  implicit def stringToPost(s: String): Seq[BlogPost] = {
    val parser = new DOMParser

    val tree = parser.parseFromString(s, "text/xml")

    val rssItems = tree.getElementsByTagName("item")

    (for {
      i ← 0 to rssItems.length - 1
      nodes = rssItems(i).childNodes
      ns ← 0 to nodes.length - 1
    } yield {
      val node = nodes.item(ns)
      ReadNode(i, node.nodeName, node.textContent)
    }).filter { rn ⇒ searchedTags.contains(rn.name)
    }.groupBy(_.index).values.map { readNodeToBlogPost }.toSeq
  }

  val newsStyle = Seq(
    backgroundColor := "#333",
    padding := 10,
    marginTop := 5,
    borderRadius := "5px"
  )

  val titleStyle = Seq(
    textTransform := "uppercase",
    color := "white"
  )

  val moreStyle = Seq(
    float := "right",
    right := 10
  )

  def testAndAppend(id: String, element: HTMLElement) = {
    val node = org.scalajs.dom.window.document.getElementById(id)
    if (node != null)
      node.appendChild(element)
  }

  def addNewsdiv(blogPosts: Seq[BlogPost]) = {
    val newsDiv = div(paddingTop := 20)(
      h2("News"),
      for {
        bp ← blogPosts
      } yield {
        div(
          span(bp.title)(titleStyle),
          span(a(href := bp.link, target := "_blank")("Read more"))(moreStyle)
        )(newsStyle)
      }
    ).render

    testAndAppend(shared.newsPosts, newsDiv)
  }

  def addTrainings(blogPosts: Seq[BlogPost], id: String) = {
    val trainingDiv = div(
      for {
        bp ← blogPosts
      } yield {
        div(
          a(href := bp.link, target := "_blank")(bp.title)
        )
      }
    ).render

    testAndAppend(id, trainingDiv)
  }
}

