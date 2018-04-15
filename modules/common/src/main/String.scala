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

    def nl2brUnsafe(text: String) = Html {
      text.replace("\r\n", "<br />").replace("\n", "<br />")
    }

    def nl2br(text: String): Html = nl2brUnsafe(StringUtils.escapeHtml(text))

    def richText(text: String, nl2br: Boolean = true): Html = {
      val withUsernames = addUserProfileLinks(StringUtils.escapeHtml(text))
      val withLinks = addLinks(withUsernames)
      if (nl2br) nl2brUnsafe(withLinks) else Html(withLinks)
    }

    // has negative lookbehind to exclude overlaps with user profile links
    private val urlRegex = """(?i)(?<![">])\b((https?:\/\/|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,6}\/)((?:[`!\[\]{};:'".,<>?«»“”‘’]*[^\s`!\[\]{}\(\);:'".,<>?«»“”‘’])*))""".r
    // private val imgRegex = """(?:(?:https?:\/\/))[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,4}\b(?:[-a-zA-Z0-9@:%_\+.~#?&\/=]*(\.jpg|\.png|\.jpeg))""".r
    private val netDomain = "lichess.org" // whatever...

    private val urlMustNotContain = List("&quot")

    private def addUserProfileLinks(text: String): String =
      atUsernameRegex.replaceAllIn(text, m => {
        val user = m group 1
        s"""<a href="/@/$user">@$user</a>"""
      })

    private def addLinks(text: String): String = try {
      urlRegex.replaceAllIn(text, m => {
        if (urlMustNotContain exists m.group(0).contains) m.group(0)
        else if (m.group(2) == "http://" || m.group(2) == "https://") {
          if (s"${m.group(3)}/" startsWith s"$netDomain/") {
            // internal
            val link = m.group(3)
            s"""<a href="//$link">${urlOrImg(link)}</a>"""
          } else {
            // external
            val link = m.group(1)
            s"""<a rel="nofollow" href="$link" target="_blank">${urlOrImg(link)}</a>"""
          }
        } else {
          if (s"${m.group(2)}/" startsWith s"$netDomain/") {
            // internal
            val link = m.group(1)
            s"""<a href="//$link">${urlOrImg(link)}</a>"""
          } else {
            // external
            val link = m.group(1)
            s"""<a rel="nofollow" href="http://$link" target="_blank">${urlOrImg(link)}</a>"""
          }
        }
      })
    } catch {
      case e: IllegalArgumentException =>
        lila.log("templating").error(s"addLinks($text)", e)
        text
      case e: StackOverflowError =>
        lila.log("templating").error(text take 10000, e)
        text
    }

    private val imgUrlPattern = """.*\.(jpg|jpeg|png|gif)$""".r.pattern
    private val imgurRegex = """https?://imgur\.com/(\w+)""".r

    private def urlToImg(url: String): Option[String] = (url match {
      case imgurRegex(id) => s"""https://i.imgur.com/$id.jpg""".some
      case u if imgUrlPattern.matcher(url).matches && !url.contains(s"://$netDomain") => u.some
      case _ => none
    }) map { imgUrl => s"""<img class="embed" src="$imgUrl"/>""" }

    private def urlOrImg(url: String): String = urlToImg(url) getOrElse url

    private val markdownLinkRegex = """\[([^\[]+)\]\(([^\)]+)\)""".r

    def markdownLinks(text: String): Html = nl2brUnsafe {
      markdownLinkRegex.replaceAllIn(StringUtils.escapeHtml(text), m => {
        val href = m.group(2)
        if (href.startsWith("http://") || href.startsWith("https://"))
          s"""<a href="$href">${m group 1}</a>"""
        else m.group(0)
      })
    }

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

    def escapeHtml(s: String): Html = Html(StringUtils.escapeHtml(s))
  }
}
