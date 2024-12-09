package lila.common

import play.api.libs.json.*
import scalalib.StringUtils.{ escapeHtmlRaw, safeJsonString }
import scalatags.Text.all.*

import lila.core.config.NetDomain
import lila.core.data.SafeJsonStr

object String:

  export RawHtml.hasLinks
  export scalalib.StringOps.*

  def lcfirst(str: String) = s"${str(0).toLower}${str.drop(1)}"

  def decodeUriPath(input: String): Option[String] =
    try play.utils.UriEncoding.decodePath(input, "UTF-8").some
    catch case _: play.utils.InvalidUriEncodingException => None

  def decodeUriPathSegment(input: String): Option[String] =
    try play.utils.UriEncoding.decodePathSegment(input, "UTF-8").some
    catch case _: play.utils.InvalidUriEncodingException => None

  def isShouting(text: String) =
    text.lengthIs >= 5 && {
      import java.lang.Character.*
      // true if >1/2 of the latin letters are uppercase
      text.take(80).replace("O-O", "o-o").foldLeft(0) { (i, c) =>
        getType(c) match
          case UPPERCASE_LETTER => i + 1
          case LOWERCASE_LETTER => i - 1
          case _                => i
      } > 0
    }
  def noShouting(str: String): String = if isShouting(str) then str.toLowerCase else str

  val atUsernameRegex    = RawHtml.atUsernameRegex
  val forumPostPathRegex = """(?:(?<= )|^)\b([\w-]+/[\w-]+)\b(?:(?= )|$)""".r

  object html:

    inline def raw(inline html: Html) = scalatags.Text.all.raw(html.value)

    def richText(rawText: String, nl2br: Boolean = true, expandImg: Boolean = true)(using NetDomain): Frag =
      raw:
        val withLinks = RawHtml.addLinks(rawText, expandImg)
        if nl2br then RawHtml.nl2br(withLinks.value) else withLinks

    def nl2brUnsafe(text: String): Frag =
      raw:
        RawHtml.nl2br(text)

    def nl2br(text: String): Frag = nl2brUnsafe(escapeHtmlRaw(text))

    def escapeHtml(h: Html): RawFrag =
      raw:
        Html(escapeHtmlRaw(h.value))

    def unescapeHtml(html: Html): Html =
      html.map(org.apache.commons.text.StringEscapeUtils.unescapeHtml4)

    def markdownLinksOrRichText(text: String)(using NetDomain): Frag =
      val escaped = Html(escapeHtmlRaw(text))
      val marked  = RawHtml.justMarkdownLinks(escaped)
      if marked == escaped then richText(text)
      else nl2brUnsafe(marked.value)

    def safeJsonValue(jsValue: JsValue): SafeJsonStr = SafeJsonStr:
      // Borrowed from:
      // https://github.com/playframework/play-json/blob/160f66a84a9c5461c52b50ac5e222534f9e05442/play-json/js/src/main/scala/StaticBinding.scala#L65
      jsValue match
        case JsNull         => "null"
        case JsString(s)    => safeJsonString(s)
        case JsNumber(n)    => n.toString
        case JsFalse        => "false"
        case JsTrue         => "true"
        case JsArray(items) => items.map(safeJsonValue).mkString("[", ",", "]")
        case JsObject(fields) =>
          fields
            .map: (k, v) =>
              s"${safeJsonString(k)}:${safeJsonValue(v)}"
            .mkString("{", ",", "}")

  object charset:
    import akka.util.ByteString
    import com.ibm.icu.text.CharsetDetector

    def guessAndDecode(str: ByteString): String =
      str.decodeString(guess(str) | "UTF-8")

    def guess(str: ByteString): Option[String] =
      Option:
        val cd = new CharsetDetector
        cd.setText(str.take(10000).toArray)
        cd.detect()
      .map(_.getName)
