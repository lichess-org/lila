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

  def decodeUriPath(input: String): Option[String] = {
    try {
      play.utils.UriEncoding.decodePath(input, "UTF-8").some
    } catch {
      case _: play.utils.InvalidUriEncodingException => None
    }
  }

  private[this] def oneline(s: String) = s.replace('\n', ' ')
  def shorten(text: String, length: Int, sep: String = "…") = {
    if (text.lengthIs > length + sep.length) oneline(text take length) ++ sep
    else oneline(text)
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

  def hasLinks = RawHtml.hasLinks _

  def hasGarbageChars(s: String) = hasZeroWidthChars(s) || hasInvisibleChars(s)

  private def hasZeroWidthChars(s: String) =
    s.contains('\u200b') ||
      s.contains('\u200c') ||
      s.contains('\u200d') ||
      s.contains('\u200e') ||
      s.contains('\u200f') ||
      s.contains('\u202e') // https://www.fileformat.info/info/unicode/char/202e/index.htm

  private def hasInvisibleChars(s: String) =
    s.contains('\u1160') // https://www.compart.com/en/unicode/U+1160

  object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String): Option[String] =
      try {
        Some(new String(Base64.getDecoder decode txt, StandardCharsets.UTF_8))
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

    def markdownLinksOrRichText(text: String): Frag = {
      val escaped = escapeHtmlRaw(text)
      val marked  = RawHtml.justMarkdownLinks(escaped)
      if (marked == escaped) richText(text)
      else nl2brUnsafe(marked)
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
        case JsObject(fields) =>
          fields
            .map { case (k, v) =>
              s"${safeJsonString(k)}:${safeJsonValue(v)}"
            }
            .mkString("{", ",", "}")
      }
    }
  }

  private val prizeRegex =
    """(?i)(prize|\$|€|£|¥|₽|元|₹|₱|₿|rupee|rupiah|ringgit|usd|dollar|paypal|cash|award|\bfees?\b)""".r.unanchored

  def looksLikePrize(txt: String) = prizeRegex matches txt
}
