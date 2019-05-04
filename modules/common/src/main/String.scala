package lila.common

import java.text.Normalizer
import play.api.libs.json._
import scalatags.Text.all._

import lila.base.RawHtml
import lila.common.base.StringUtils.{ safeJsonString, escapeHtmlRaw }

final object String {

  private[this] val slugR = """[^\w-]""".r
  private[this] val slugMultiDashRegex = """-{2,}""".r

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(' ', '-')
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
    val t = text.replace('\n', ' ')
    if (t.size > (length + sep.size)) (t take length) ++ sep
    else t
  }

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

  val atUsernameRegex = RawHtml.atUsernameRegex

  object html {
    def richText(rawText: String, nl2br: Boolean = true): Frag = raw {
      val withLinks = RawHtml.addLinks(rawText)
      if (nl2br) RawHtml.nl2br(withLinks) else withLinks
    }

    def nl2brUnsafe(text: String): Frag = raw {
      RawHtml nl2br text
    }

    def nl2br(text: String): Frag = nl2brUnsafe(escapeHtmlRaw(text))

    def escapeHtml(s: String): RawFrag = raw {
      escapeHtmlRaw(s)
    }

    def markdownLinks(text: String): Frag = raw {
      RawHtml.markdownLinks(text)
    }

    def safeJsonValue(jsValue: JsValue): String = {
      // Borrowed from:
      // https://github.com/playframework/play-json/blob/160f66a84a9c5461c52b50ac5e222534f9e05442/play-json/js/src/main/scala/StaticBinding.scala#L65
      jsValue match {
        case JsNull => "null"
        case JsString(s) => safeJsonString(s)
        case JsNumber(n) => n.toString
        case JsBoolean(b) => if (b) "true" else "false"
        case JsArray(items) => items.map(safeJsonValue).mkString("[", ",", "]")
        case JsObject(fields) => {
          fields.map {
            case (k, v) => s"${safeJsonString(k)}:${safeJsonValue(v)}"
          }.mkString("{", ",", "}")
        }
      }
    }
  }
}
