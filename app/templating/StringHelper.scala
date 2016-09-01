package lila.app
package templating

import lila.user.{ User, UserContext }
import play.twirl.api.Html

trait StringHelper { self: NumberHelper =>

  def netDomain: String

  val escapeHtml: String => String = org.apache.commons.lang3.StringEscapeUtils.escapeHtml4 _

  val slugify = lila.common.String.slugify _

  def shorten(text: String, length: Int, sep: String = "…") = Html {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) escapeHtml(t take length) ++ sep
    else escapeHtml(t)
  }

  def shortenWithBr(text: String, length: Int) = Html {
    nl2br(escapeHtml(text).take(length)).replace("<br /><br />", "<br />")
  }

  def pluralize(s: String, n: Int) = s"$n $s${if (n > 1) "s" else ""}"

  private val autoLinkFun: String => String =
    nl2br _ compose addUserProfileLinks _ compose addLinks _ compose escapeHtml

  def autoLink(text: String) = Html { autoLinkFun(text) }

  def nl2br(text: String) = text.replace("\r\n", "<br />").replace("\n", "<br />")

  private val markdownLinkRegex = """\[([^\[]+)\]\(([^\)]+)\)""".r

  def markdownLinks(text: String) = Html {
    nl2br {
      markdownLinkRegex.replaceAllIn(escapeHtml(text), m => {
        s"""<a href="${m group 2}">${m group 1}</a>"""
      })
    }
  }

  private val urlRegex = """(?i)\b((https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,6}/)((?:[^\s<>]+|\(([^\s<>]+|(\([^\s<>]+\)))*\))+(?:\(([^\s<>]+|(\([^\s<>]+\)))*\)|[^\s`!\[\]{};:'".,<>?«»“”‘’])))""".r

  /**
   * Creates hyperlinks to user profiles mentioned using the '@' prefix. e.g. @ornicar
   * @param text The text to regex match
   * @return The text as a HTML hyperlink
   */
  def addUserProfileLinks(text: String) = User.atUsernameRegex.replaceAllIn(text, m => {
    val user = m group 1
    val url = s"//$netDomain/@/$user"

    s"""<a href="$url">@$user</a>"""
  })

  def addLinks(text: String) = try {
    urlRegex.replaceAllIn(text, m => {
      if (m.group(0) contains "&quot") m.group(0)
      else if (m.group(2) == "http://" || m.group(2) == "https://") {
        if (s"${delocalize(m.group(3))}/" startsWith s"$netDomain/") {
          // internal
          val link = delocalize(m.group(3))
          s"""<a rel="nofollow" href="//$link">$link</a>"""
        }
        else {
          // external
          val link = m.group(1)
          s"""<a rel="nofollow" href="$link" target="_blank">$link</a>"""
        }
      }
      else {
        if (s"${delocalize(m.group(2))}/" startsWith s"$netDomain/") {
          // internal
          val link = delocalize(m.group(1))
          s"""<a rel="nofollow" href="//$link">$link</a>"""
        }
        else {
          // external
          val link = m.group(1)
          s"""<a rel="nofollow" href="http://$link" target="_blank">$link</a>"""
        }
      }
    })
  }
  catch {
    case e: IllegalArgumentException =>
      lila.log("templating").error(s"addLinks($text)", e)
      text
  }

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

  def addQueryParameter(url: String, key: String, value: Any) =
    if (url contains "?") s"$url&$key=$value" else s"$url?$key=$value"
}
