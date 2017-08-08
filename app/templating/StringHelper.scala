package lila.app
package templating

import ornicar.scalalib.Zero
import play.twirl.api.Html

import lila.user.{ User, UserContext }

trait StringHelper { self: NumberHelper =>

  def netDomain: String

  implicit val LilaHtmlZero: Zero[Html] = Zero.instance(Html(""))

  val slugify = lila.common.String.slugify _

  val escapeHtml = lila.common.String.html.escape _

  private val escapeHtmlUnsafe = lila.common.String.html.escapeUnsafe _

  def shorten(text: String, length: Int, sep: String = "…"): Html = {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) Html(escapeHtmlUnsafe(t take length) ++ sep)
    else escapeHtml(t)
  }

  def shortenWithBr(text: String, length: Int) = Html {
    nl2brUnsafe(escapeHtmlUnsafe(text).take(length)).replace("<br /><br />", "<br />")
  }

  def pluralize(s: String, n: Int) = s"$n $s${if (n > 1) "s" else ""}"

  def autoLink(text: String): Html = nl2br(addUserProfileLinksUnsafe(addLinksUnsafe(escapeHtmlUnsafe(text))))

  private def nl2brUnsafe(text: String): String =
    text.replace("\r\n", "<br />").replace("\n", "<br />")

  def nl2br(text: String) = Html(nl2brUnsafe(text))

  private val markdownLinkRegex = """\[([^\[]+)\]\(([^\)]+)\)""".r

  def markdownLinks(text: String): Html = nl2br {
    markdownLinkRegex.replaceAllIn(escapeHtmlUnsafe(text), m => {
      s"""<a href="${m group 2}">${m group 1}</a>"""
    })
  }

  def repositionTooltip(link: Html, position: String) = Html {
    link.body.replace("<a ", s"""<a data-pt-pos="$position" """)
  }

  private val urlRegex = """(?i)\b((https?:\/\/|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,6}\/)((?:[`!\[\]{};:'".,<>?«»“”‘’]*[^\s`!\[\]{}\(\);:'".,<>?«»“”‘’])*))""".r

  /**
   * Creates hyperlinks to user profiles mentioned using the '@' prefix. e.g. @ornicar
   * @param text The text to regex match
   * @return The text as a HTML hyperlink
   */
  def addUserProfileLinks(text: String) = Html(addUserProfileLinksUnsafe(text))

  private def addUserProfileLinksUnsafe(text: String): String =
    User.atUsernameRegex.replaceAllIn(text, m => {
      val user = m group 1
      val url = s"//$netDomain/@/$user"

      s"""<a href="$url">@$user</a>"""
    })

  def addLinks(text: String) = Html(addLinksUnsafe(text))

  private def addLinksUnsafe(text: String): String = try {
    urlRegex.replaceAllIn(text, m => {
      if (m.group(0) contains "&quot") m.group(0)
      else if (m.group(2) == "http://" || m.group(2) == "https://") {
        if (s"${m.group(3)}/" startsWith s"$netDomain/") {
          // internal
          val link = m.group(3)
          s"""<a rel="nofollow" href="//$link">$link</a>"""
        } else {
          // external
          val link = m.group(1)
          s"""<a rel="nofollow" href="$link" target="_blank">$link</a>"""
        }
      } else {
        if (s"${m.group(2)}/" startsWith s"$netDomain/") {
          // internal
          val link = m.group(1)
          s"""<a rel="nofollow" href="//$link">$link</a>"""
        } else {
          // external
          val link = m.group(1)
          s"""<a rel="nofollow" href="http://$link" target="_blank">$link</a>"""
        }
      }
    })
  } catch {
    case e: IllegalArgumentException =>
      lila.log("templating").error(s"addLinks($text)", e)
      text
  }

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
      case NumberFirstRegex(number, text) =>
        s"<strong>${(~parseIntOption(number)).localize}</strong><br />$text"
      case NumberLastRegex(text, number) =>
        s"$text<br /><strong>${(~parseIntOption(number)).localize}</strong>"
      case h => h.replace("\n", "<br />")
    }
  }
  def splitNumber(s: Html)(implicit ctx: UserContext): Html = splitNumber(s.body)

  def encodeFen(fen: String) = lila.common.String.base64.encode(fen).reverse

  def addQueryParameter(url: String, key: String, value: Any) =
    if (url contains "?") s"$url&$key=$value" else s"$url?$key=$value"

  def htmlList(htmls: List[Html], separator: String = ", ") = Html {
    htmls mkString separator
  }
}
