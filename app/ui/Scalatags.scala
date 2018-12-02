package lidraughts.app
package ui

import play.twirl.api.Html
import scalatags.Text.TypedTag

object Scalatags {

  implicit def LidraughtsScalaTagsToHtml(tags: TypedTag[String]): Html = Html {
    tags.render
  }
}
