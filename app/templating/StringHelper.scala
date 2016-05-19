package lila.app
package templating

import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Matcher.quoteReplacement

import lila.user.UserContext
import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import play.twirl.api.Html

trait StringHelper { self: NumberHelper =>

  def netDomain: String

  val slugify = lila.common.String.slugify _

  def shorten(text: String, length: Int, sep: String = "…") = Html {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) escape(t take length) ++ sep
    else escape(t)
  }

  def shortenWithBr(text: String, length: Int) = Html {
    nl2br(escape(text).take(length)).replace("<br /><br />", "<br />")
  }

  def pluralize(s: String, n: Int) = "%d %s%s".format(n, s, if (n > 1) "s" else "")

  def autoLink(text: String) = Html { (nl2br _ compose addUserProfileLinks _ compose addLinks _ compose escape _)(text) }

  // the replace quot; -> " is required
  // to avoid issues caused by addLinks
  // when an url is surrounded by quotes
  def escape(text: String) = escapeEvenDoubleQuotes(text).replace("&quot;", "\"")
  def escapeEvenDoubleQuotes(text: String) = escapeHtml4(text)

  def nl2br(text: String) = text.replace("\r\n", "<br />").replace("\n", "<br />")

  private val markdownLinkRegex = """\[([^\[]+)\]\(([^\)]+)\)""".r

  def markdownLinks(text: String) = Html {
    nl2br {
      markdownLinkRegex.replaceAllIn(escape(text), m => {
        s"""<a href="${m group 2}">${m group 1}</a>"""
      })
    }
  }

  // Matches a lichess username with a '@' prefix (I hope.) Example: @ornicar
  private val atUsernameRegex = "(?<=^|(?<=[^a-zA-Z0-9-_\\.]))@([A-Za-z]+[A-Za-z0-9]+)".r

  private val urlRegex = """(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s<>]+|\(([^\s<>]+|(\([^\s<>]+\)))*\))+(?:\(([^\s<>]+|(\([^\s<>]+\)))*\)|[^\s`!\[\]{};:'".,<>?«»“”‘’]))""".r

  /**
    * Creates hyperlinks to user profiles mentioned using the '@' prefix. e.g. @ornicar
    * @param text The text to regex match
    * @return The text as a HTML hyperlink
    */
  def addUserProfileLinks(text: String) = atUsernameRegex.replaceAllIn(text, m => {
    var user = m group 1
    var url = "lichess.org/@/" ++ user

    s"""<a href="${prependHttp(url)}">@$user</a>"""
  })

  def addLinks(text: String) = urlRegex.replaceAllIn(text, m => {
    val url = delocalize(quoteReplacement(m group 1))
    val target = if (url contains netDomain) "" else " target='blank'"
    s"""<a$target rel="nofollow" href="${prependHttp(url)}">$url</a>"""
  })

  private def prependHttp(url: String): String =
    url startsWith "http" fold (url, "http://" + url)

  private val delocalize = new lila.common.String.Delocalizer(netDomain)

  def showNumber(n: Int): String = if (n > 0) s"+$n" else n.toString

  implicit def lilaRichString(str: String) = new {
    def active(other: String, one: String = "active") = if (str == other) one else ""
  }

  def when(cond: Boolean, str: String) = cond ?? str
  def strong(x: Int): String = strong(x.toString)
  def strong(x: String): String = s"<strong>$x</strong>"

  private val NumberFirstRegex = """^(\d+)\s(.+)$""".r
  private val NumberLastRegex = """^(.+)\s(\d+)$""".r
  def splitNumber(s: String)(implicit ctx: UserContext): Html = Html {
    s match {
      case NumberFirstRegex(number, text) => "<strong>%s</strong><br />%s".format((~parseIntOption(number)).localize, text)
      case NumberLastRegex(text, number)  => "%s<br /><strong>%s</strong>".format(text, (~parseIntOption(number)).localize)
      case h                              => h.replace("\n", "<br />")
    }
  }
  def splitNumber(s: Html)(implicit ctx: UserContext): Html = splitNumber(s.body)

  private def base64encode(str: String) = {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    Base64.getEncoder.encodeToString(str getBytes StandardCharsets.UTF_8)
  }

  def encodeFen(fen: String) = base64encode(fen).reverse
}
