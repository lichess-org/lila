package lila.common

import java.text.Normalizer
import play.api.libs.json._
import play.twirl.api.Html

import lila.common.base.StringUtils

object String {

  private val slugR = """[^\w-]""".r
  private val slugMultiDashRegex = """-{2,}""".r

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val singleDashes = slugMultiDashRegex.replaceAllIn(nowhitespace, "-")
    val normalized = Normalizer.normalize(singleDashes, Normalizer.Form.NFD)
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

  def levenshtein(s1: String, s2: String): Int = StringUtils.levenshtein(s1, s2)

  object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String): Option[String] = try {
      Some(new String(Base64.getDecoder decode txt, StandardCharsets.UTF_8))
    } catch {
      case _: java.lang.IllegalArgumentException => none
    }
  }

  // Matches a lichess username with an '@' prefix if it is used as a single
  // word (i.e. preceded and followed by space or appropriate punctuation):
  // Yes: everyone says @ornicar is a pretty cool guy
  // No: contact@lichess.org, @1, http://example.com/@happy0
  val atUsernameRegex = """(?<=^|[^\w@#/])@(?>([\w-]{2,20}))(?![@\w-])""".r

  object html {

    private def nl2brUnsafe(text: String): String =
      text.replace("\r\n", "<br />").replace("\n", "<br />")

    def nl2br(text: String) = Html(nl2brUnsafe(text))

    def shortenWithBr(text: String, length: Int) = Html {
      nl2brUnsafe(escapeHtmlUnsafe(text.take(length))).replace("<br /><br />", "<br />")
    }

    def shorten(text: String, length: Int, sep: String = "…"): Html = {
      val t = text.replace("\n", " ")
      if (t.size > (length + sep.size)) Html(escapeHtmlUnsafe(t.take(length) ++ sep))
      else escapeHtml(t)
    }

    def autoLink(text: String): Html = Html(nl2brUnsafe(addUserProfileLinksUnsafe(addLinksUnsafe(escapeHtmlUnsafe(text)))))
    private val urlRegex = """(?i)\b((https?:\/\/|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,6}\/)((?:[`!\[\]{};:'".,<>?«»“”‘’]*[^\s`!\[\]{}\(\);:'".,<>?«»“”‘’])*))""".r
    // private val imgRegex = """(?:(?:https?:\/\/))[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,4}\b(?:[-a-zA-Z0-9@:%_\+.~#?&\/=]*(\.jpg|\.png|\.jpeg))""".r
    private val netDomain = "lichess.org" // whatever...
    private val urlMustNotContain = List("&quot", "@")

    /**
     * Creates hyperlinks to user profiles mentioned using the '@' prefix. e.g. @ornicar
     * @param text The text to regex match
     * @return The text as a HTML hyperlink
     */
    def addUserProfileLinks(html: Html) = Html(addUserProfileLinksUnsafe(html.body))

    private def addUserProfileLinksUnsafe(text: String): String =
      atUsernameRegex.replaceAllIn(text, m => {
        val user = m group 1
        s"""<a href="/@/$user">@$user</a>"""
      })

    def addLinks(html: Html) = Html(addLinksUnsafe(html.body))

    private def addLinksUnsafe(text: String): String = try {
      urlRegex.replaceAllIn(text, m => {
        if (urlMustNotContain exists m.group(0).contains) m.group(0)
        else if (m.group(2) == "http://" || m.group(2) == "https://") {
          if (s"${m.group(3)}/" startsWith s"$netDomain/") {
            // internal
            val link = m.group(3)
            s"""<a href="//$link">${urlOrImgUnsafe(link)}</a>"""
          } else {
            // external
            val link = m.group(1)
            s"""<a rel="nofollow" href="$link" target="_blank">${urlOrImgUnsafe(link)}</a>"""
          }
        } else {
          if (s"${m.group(2)}/" startsWith s"$netDomain/") {
            // internal
            val link = m.group(1)
            s"""<a href="//$link">${urlOrImgUnsafe(link)}</a>"""
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
    private val imgurRegex = """https?://imgur\.com/(\w+)""".r

    private def urlToImgUnsafe(url: String): Option[String] = (url match {
      case imgurRegex(id) => s"""https://i.imgur.com/$id.jpg""".some
      case u if imgUrlPattern.matcher(url).matches && !url.contains(s"://$netDomain") => u.some
      case _ => none
    }) map { imgUrl => s"""<img class="embed" src="$imgUrl"/>""" }

    private def urlOrImgUnsafe(url: String): String = urlToImgUnsafe(url) getOrElse url

    val escapeHtmlUnsafe = StringUtils.escapeHtmlUnsafe _

    // from https://github.com/android/platform_frameworks_base/blob/d59921149bb5948ffbcb9a9e832e9ac1538e05a0/core/java/android/text/TextUtils.java#L1361
    def escapeHtml(s: String): Html = Html(escapeHtmlUnsafe(s))

    private val markdownLinkRegex = """\[([^\[]+)\]\(([^\)]+)\)""".r

    def markdownLinks(text: String): Html = Html(nl2brUnsafe {
      markdownLinkRegex.replaceAllIn(escapeHtmlUnsafe(text), m => {
        s"""<a href="${m group 2}">${m group 1}</a>"""
      })
    })

    def safeJsonValue(jsValue: JsValue): String = {
      // Borrowed from:
      // https://github.com/playframework/play-json/blob/160f66a84a9c5461c52b50ac5e222534f9e05442/play-json/js/src/main/scala/StaticBinding.scala#L65
      jsValue match {
        case JsNull => "null"
        case JsString(s) => StringUtils.safeJsonString(s)
        case JsNumber(n) => n.toString
        case JsBoolean(b) => if (b) "true" else "false"
        case JsArray(items) => items.map(safeJsonValue).mkString("[", ",", "]")
        case JsObject(fields) => {
          fields.map {
            case (key, value) =>
              s"${StringUtils.safeJsonString(key)}:${safeJsonValue(value)}"
          }.mkString("{", ",", "}")
        }
      }
    }

    def safeJson(jsValue: JsValue): Html = Html(safeJsonValue(jsValue))
  }
}
