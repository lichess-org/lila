package lila
package templating

import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import play.api.templates.Html

object StringHelper extends StringHelper

trait StringHelper {

  def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def shorten(text: String, length: Int): String =
    text.replace("\n", " ") take length

  def pluralize(s: String, n: Int) = "%d %s%s".format(n, s, if (n > 1) "s" else "")

  def autoLink(text: String) = Html {
    addLinks(escape(text)).replace("\n", "<br />")
  }

  def escape(text: String) = escapeXml(text)

  private val urlRegex = """(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))""".r

  def addLinks(text: String) = urlRegex.replaceAllIn(text, m ⇒
    "<a href='%s'>%s</a>".format(m group 1, m group 1))

  def showNumber(n: Int): String = (n > 0).fold("+" + n, n.toString)

  implicit def richString(str: String) = new {
    def active(other: String, then: String = "active") = 
      (str == other).fold(then, "")
  }
}
