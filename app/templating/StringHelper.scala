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
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def shorten(text: String, length: Int, sep: String = " [...]"): String = {
    val t = text.replace("\n", " ") 
    if (t.size > (length + sep.size)) (t take length) ++ sep
    else t
  }

  def shortenWithBr(text: String, length: Int) = Html {
    nl2br(escape(text).take(length))
  }

  def pluralize(s: String, n: Int) = "%d %s%s".format(n, s, if (n > 1) "s" else "")

  def autoLink(text: String) = Html {
    nl2br(addLinks(escape(text)))
  }

  def escape(text: String) = escapeXml(text)

  def nl2br(text: String) = text.replace("\r\n", "<br />").replace("\n", "<br />")

  private val urlRegex = """(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))""".r

  def addLinks(text: String) = urlRegex.replaceAllIn(text, m ⇒
    "<a href='%s'>%s</a>".format(m group 1, m group 1))

  def showNumber(n: Int): String = (n > 0).fold("+" + n, n.toString)

  implicit def richString(str: String) = new {
    def active(other: String, then: String = "active") = 
      (str == other).fold(then, "")
  }

  def strong(x: Int): String = strong(x.toString)
  def strong(x: String): String = "<strong>" + x + "</strong>"
}
