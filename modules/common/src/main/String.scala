package lila.common

import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement
import play.twirl.api.Html

object String {

  private val slugR = """[^\w-]""".r

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def decodeUriPath(input: String): Option[String] = {
    try {
      play.utils.UriEncoding.decodePath(input, "UTF-8").some
    } catch {
      case e: play.utils.InvalidUriEncodingException => None
    }
  }

  def shorten(text: String, length: Int, sep: String = "…") = {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) (t take length) ++ sep
    else t
  }

  object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String): Option[String] = try {
      Some(new String(Base64.getDecoder decode txt))
    } catch {
      case _: java.lang.IllegalArgumentException => none
    }
  }

  // Matches a lichess username with an '@' prefix if it is used as a single
  // word (i.e. preceded and followed by space or appropriate punctuation):
  // Yes: everyone says @ornicar is a pretty cool guy
  // No: contact@lichess.org, @1, http://example.com/@happy0
  val atUsernameRegex = """(?<=\s|^)@(?>([a-zA-Z_-][\w-]{1,19}))(?![\w-])""".r

  object html {

    private def nl2brUnsafe(text: String): String =
      text.replace("\r\n", "<br />").replace("\n", "<br />")

    def nl2br(text: String) = Html(nl2brUnsafe(text))

    def shortenWithBr(text: String, length: Int) = Html {
      nl2brUnsafe(escapeHtmlUnsafe(text).take(length)).replace("<br /><br />", "<br />")
    }

    def shorten(text: String, length: Int, sep: String = "…"): Html = {
      val t = text.replace("\n", " ")
      if (t.size > (length + sep.size)) Html(escapeHtmlUnsafe(t take length) ++ sep)
      else escapeHtml(t)
    }

    def autoLink(text: String): Html = nl2br(addUserProfileLinksUnsafe(addLinksUnsafe(escapeHtmlUnsafe(text))))
    private val urlRegex = """(?i)\b((https?:\/\/|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,6}\/)((?:[`!\[\]{};:'".,<>?«»“”‘’]*[^\s`!\[\]{}\(\);:'".,<>?«»“”‘’])*))""".r
    // private val imgRegex = """(?:(?:https?:\/\/))[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,4}\b(?:[-a-zA-Z0-9@:%_\+.~#?&\/=]*(\.jpg|\.png|\.jpeg))""".r
    private val netDomain = "lichess.org" // whatever...

    /**
     * Creates hyperlinks to user profiles mentioned using the '@' prefix. e.g. @ornicar
     * @param text The text to regex match
     * @return The text as a HTML hyperlink
     */
    def addUserProfileLinks(text: String) = Html(addUserProfileLinksUnsafe(text))

    private def addUserProfileLinksUnsafe(text: String): String =
      atUsernameRegex.replaceAllIn(text, m => {
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
            s"""<a rel="nofollow" href="//$link">${urlOrImgUnsafe(link)}</a>"""
          } else {
            // external
            val link = m.group(1)
            s"""<a rel="nofollow" href="$link" target="_blank">${urlOrImgUnsafe(link)}</a>"""
          }
        } else {
          if (s"${m.group(2)}/" startsWith s"$netDomain/") {
            // internal
            val link = m.group(1)
            s"""<a rel="nofollow" href="//$link">${urlOrImgUnsafe(link)}</a>"""
          } else {
            // external
            val link = m.group(1)
            s"""<a rel="nofollow" href="http://$link" target="_blank">${urlOrImgUnsafe(link)}</a>"""
          }
        }
      })
    } catch {
      case e: IllegalArgumentException =>
        lila.log("templating").error(s"addLinks($text)", e)
        text
    }

    private val imgUrlPattern = """.*\.(jpg|jpeg|png|gif)$""".r.pattern

    private def urlToImgUnsafe(url: String): Option[String] = {
      imgUrlPattern.matcher(url).matches && !url.contains(s"://$netDomain")
    } option s"""<img class="embed" src="$url" style="max-width:100%" />"""

    private def urlOrImgUnsafe(url: String) = urlToImgUnsafe(url) getOrElse url

    // from https://github.com/android/platform_frameworks_base/blob/d59921149bb5948ffbcb9a9e832e9ac1538e05a0/core/java/android/text/TextUtils.java#L1361
    def escapeHtml(s: String): Html = Html(escapeHtmlUnsafe(s))

    private val badChars = "[<>&\"']".r.pattern

    def escapeHtmlUnsafe(s: String): String = {
      if (badChars.matcher(s).find) {
        val sb = new StringBuilder(s.size + 10) // wet finger style
        var i = 0
        while (i < s.length) {
          sb.append {
            s.charAt(i) match {
              case '<' => "&lt;"
              case '>' => "&gt;"
              case '&' => "&amp;"
              case '"' => "&quot;"
              case '\'' => "&#39;"
              case c => c
            }
          }
          i += 1
        }
        sb.toString
      } else s
    }

    private val markdownLinkRegex = """\[([^\[]+)\]\(([^\)]+)\)""".r

    def markdownLinks(text: String): Html = nl2br {
      markdownLinkRegex.replaceAllIn(escapeHtmlUnsafe(text), m => {
        s"""<a href="${m group 2}">${m group 1}</a>"""
      })
    }
  }
}
