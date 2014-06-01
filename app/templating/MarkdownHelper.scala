package lila.app
package templating

import com.roundeights.hasher.Implicits._
import eu.henkelmann.actuarius.ActuariusTransformer
import play.twirl.api.Html

trait MarkdownHelper {

  private val cache = collection.mutable.Map[String, Html]()

  def markdown(text: String): Html = cache.getOrElseUpdate(text.md5, doMarkdown(text))

  def markdown(text: Html): Html = markdown(text.toString)

  private def doMarkdown(text: String) = Html {
    new ActuariusTransformer() apply text
  }
}
