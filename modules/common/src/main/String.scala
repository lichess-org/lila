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
      import chess.format.Fen
      import chess.variant.Crazyhouse
      // true if >1/2 of the latin letters are uppercase (or castling notation / fen board field)
      text
        .take(1000)
        .split("\\s+")
        .filter(_.nonEmpty)
        .map(word =>
          if Set("O-O", "O-O-O").contains(word.filter(c => c.isLetter || c == '-')) || (
              word.length < 100 &&
                Set(8, 9).contains(word.split('/').count(_.nonEmpty)) &&
                Fen.makeBoard(Crazyhouse, word).isDefined
            )
          then word.toLowerCase
          else word
        )
        .mkString
        .take(80)
        .foldLeft(0) { (i, c) =>
          getType(c) match
            case UPPERCASE_LETTER => i + 1
            case LOWERCASE_LETTER => i - 1
            case _ => i
        } > 0
    }

  def noShouting(str: String): String = if isShouting(str) then str.toLowerCase else str

  val forumPostPathRegex = """(?:(?<= )|^)\b([\w-]+/[\w-]+)\b(?:(?= )|$)""".r

  object html:

    def richText(rawText: String, nl2br: Boolean = true, expandImg: Boolean = true)(using NetDomain): Frag =
      val withLinks = RawHtml.addLinks(rawText, expandImg)
      if nl2br then RawHtml.nl2br(withLinks.value).frag else withLinks.frag

    def nl2brUnsafe(text: String): Frag =
      RawHtml.nl2br(text).frag

    def nl2br(text: String): Frag = nl2brUnsafe(escapeHtmlRaw(text))

    def escapeHtml(h: Html): RawFrag =
      Html(escapeHtmlRaw(h.value)).frag

    def unescapeHtml(html: Html): Html =
      html.map(org.apache.commons.text.StringEscapeUtils.unescapeHtml4)

    def markdownLinksOrRichText(text: String)(using NetDomain): Frag =
      val escaped = Html(escapeHtmlRaw(text))
      val marked = RawHtml.justMarkdownLinks(escaped)
      if marked == escaped then richText(text)
      else nl2brUnsafe(marked.value)

    def safeJsonValue(jsValue: JsValue): SafeJsonStr = SafeJsonStr:
      val sb = java.lang.StringBuilder()
      val stack = scala.collection.mutable.ArrayDeque.empty[JsValue | String]
      stack += jsValue
      while stack.nonEmpty do
        stack.removeLast() match
          case s: String => sb.append(s)
          case JsNull => sb.append("null")
          case JsString(s) => sb.append(safeJsonString(s))
          case JsNumber(n) => sb.append(n.toString)
          case JsFalse => sb.append("false")
          case JsTrue => sb.append("true")
          case JsArray(items) =>
            // Push in reverse so LIFO pops yield: [ item0 , item1 , … ]
            // A comma precedes every item except the one pushed last (item0).
            stack += "]"
            var rest = false
            items.reverseIterator.foreach: item =>
              if rest then stack += ","
              stack += item
              rest = true
            stack += "["
          case JsObject(fields) =>
            stack += "}"
            var rest = false
            fields.toSeq.reverseIterator.foreach: (k, v) =>
              if rest then stack += ","
              stack += v
              stack += s"${safeJsonString(k)}:"
              rest = true
            stack += "{"
      sb.toString
