package lila.app
package ui

import play.twirl.api.Html
import scalatags.Text.TypedTag

object Scalatags {

  implicit def LilaScalaTagsToHtml(tags: TypedTag[String]): Html = Html {
    tags.render
  }
}
