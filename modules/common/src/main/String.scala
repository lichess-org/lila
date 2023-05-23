package lila.common

import java.text.Normalizer
import play.api.libs.json._
import scalatags.Text.all._

import lila.base.RawHtml
import lila.common.base.StringUtils.{ escapeHtmlRaw, safeJsonString }

object String {

  private[this] val slugR              = """[^\w-]""".r
  private[this] val slugMultiDashRegex = """-{2,}""".r

  def lcfirst(str: String) = s"${str(0).toLower}${str.drop(1)}"

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(' ', '-')
    val singleDashes = slugMultiDashRegex.replaceAllIn(nowhitespace, "-")
    val normalized   = Normalizer.normalize(singleDashes, Normalizer.Form.NFD)
    val slug         = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def urlencode(str: String): String = java.net.URLEncoder.encode(str, "UTF-8")

  def getBytesShiftJis(str: String) =
    try {
      str.getBytes("SHIFT-JIS")
    } catch {
      case _: java.io.UnsupportedEncodingException => Array.empty[Byte]
    }

  def decodeUriPath(input: String): Option[String] = {
    try {
      play.utils.UriEncoding.decodePath(input, "UTF-8").some
    } catch {
      case _: play.utils.InvalidUriEncodingException => None
    }
  }

  private val onelineR = """\s+""".r
  def shorten(text: String, length: Int, sep: String = "…") = {
    val oneline = onelineR.replaceAllIn(text, " ")
    if (oneline.lengthIs > length + sep.length) oneline.take(length) ++ sep
    else oneline
  }

  def isShouting(text: String) =
    text.lengthIs >= 5 && {
      import java.lang.Character._
      // true if >1/2 of the latin letters are uppercase
      (text take 80).foldLeft(0) { (i, c) =>
        getType(c) match {
          case UPPERCASE_LETTER => i + 1
          case LOWERCASE_LETTER => i - 1
          case _                => i
        }
      } > 0
    }
  def noShouting(str: String): String = if (isShouting(str)) str.toLowerCase else str

  object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets.UTF_8
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes UTF_8)
    def decode(txt: String): Option[String] =
      try {
        Some(new String(Base64.getDecoder decode txt, UTF_8))
      } catch {
        case _: java.lang.IllegalArgumentException => none
      }
  }

  val atUsernameRegex = RawHtml.atUsernameRegex

  object html {
    def richText(rawText: String, nl2br: Boolean = true): Frag =
      raw {
        val withLinks = RawHtml.addLinks(rawText)
        if (nl2br) RawHtml.nl2br(withLinks) else withLinks
      }

    def nl2brUnsafe(text: String): Frag =
      raw {
        RawHtml nl2br text
      }

    def nl2br(text: String): Frag = nl2brUnsafe(escapeHtmlRaw(text))

    def escapeHtml(s: String): RawFrag =
      raw {
        escapeHtmlRaw(s)
      }
    def unescapeHtml(html: String): String =
      org.apache.commons.text.StringEscapeUtils.unescapeHtml4(html)

    def markdownLinks(text: String): Frag =
      raw {
        RawHtml.markdownLinks(text)
      }

    def safeJsonValue(jsValue: JsValue): String = {
      // Borrowed from:
      // https://github.com/playframework/play-json/blob/160f66a84a9c5461c52b50ac5e222534f9e05442/play-json/js/src/main/scala/StaticBinding.scala#L65
      jsValue match {
        case JsNull         => "null"
        case JsString(s)    => safeJsonString(s)
        case JsNumber(n)    => n.toString
        case JsFalse        => "false"
        case JsTrue         => "true"
        case JsArray(items) => items.map(safeJsonValue).mkString("[", ",", "]")
        case JsObject(fields) => {
          fields
            .map { case (k, v) =>
              s"${safeJsonString(k)}:${safeJsonValue(v)}"
            }
            .mkString("{", ",", "}")
        }
      }
    }
  }

  private val prizeRegex =
    """(?i)(prize|\$|€|£|¥|₽|元|₹|₱|₿|rupee|rupiah|ringgit|usd|dollar|paypal|cash|award|\bfees?\b)""".r.unanchored

  def looksLikePrize(txt: String) = prizeRegex matches txt
}
