package lila.common

import java.text.Normalizer
import play.api.libs.json.*
import scalatags.Text.all.*

import lila.base.RawHtml
import lila.common.base.StringUtils.{ escapeHtmlRaw, safeJsonString }

object String:

  export RawHtml.hasLinks

  private[this] val slugR              = """[^\w-]""".r
  private[this] val slugMultiDashRegex = """-{2,}""".r

  def lcfirst(str: String) = s"${str(0).toLower}${str.drop(1)}"

  def slugify(input: String) =
    val nowhitespace = input.trim.replace(' ', '-')
    val singleDashes = slugMultiDashRegex.replaceAllIn(nowhitespace, "-")
    val normalized   = Normalizer.normalize(singleDashes, Normalizer.Form.NFD)
    val slug         = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase

  def urlencode(str: String): String = java.net.URLEncoder.encode(str, "UTF-8")

  def addQueryParam(url: String, key: String, value: String): String = addQueryParams(url, Map(key -> value))
  def addQueryParams(url: String, params: Map[String, String]): String =
    if params.isEmpty then url
    else
      val queryString = params // we could encode the key, and we should, but is it really necessary?
        .map { (key, value) => s"$key=${urlencode(value)}" }
        .mkString("&")
      s"$url${if url.contains("?") then "&" else "?"}$queryString"

  def removeChars(str: String, isRemoveable: Int => Boolean): String =
    if str.chars.anyMatch(isRemoveable(_)) then str.filterNot(isRemoveable(_)) else str

  def isGarbageChar(c: Int) = c >= '\u0250' && {
    isInvisibleChar(c) ||
    // bunch of probably useless blocks https://www.compart.com/en/unicode/block/U+2100
    // but keep maths operators cause maths are cool https://www.compart.com/en/unicode/block/U+2200
    // and chess symbols https://www.compart.com/en/unicode/block/U+2600
    (c >= '\u2100' && c <= '\u21FF') ||
    (c >= '\u2300' && c <= '\u2653') ||
    (c >= '\u2660' && c <= '\u2C5F') ||
    // decorative chars ꧁ ꧂ and svastikas
    (c == '\ua9c1' || c == '\ua9c2' || c == '\u534d' || c == '\u5350') ||
    // pretty quranic chars ۩۞
    (c >= '\u06d6' && c <= '\u06ff') ||
    // phonetic extensions https://www.compart.com/en/unicode/block/U+1D00
    (c >= '\u1d00' && c <= '\u1d7f') ||
    // IPA extensions https://www.compart.com/en/unicode/block/U+0250
    // but allow https://www.compart.com/en/unicode/U+0259
    (c >= '\u0250' && c < '\u0259') || (c > '\u0259' && c <= '\u02af')
  }

  private inline def isInvisibleChar(c: Int) =
    // invisible chars https://www.compart.com/en/unicode/block/U+2000
    (c >= '\u2000' && c <= '\u200F') ||
      // weird stuff https://www.compart.com/en/unicode/block/U+2000
      (c >= '\u2028' && c <= '\u202F') ||
      // Hangul fillers
      (c == '\u115f' || c == '\u1160')

  def removeGarbageChars(str: String) = removeChars(str, isGarbageChar)

  object normalize:

    private val ordinalRegex = "[º°ª½]".r

    // convert weird chars into letters when possible
    // but preserve ordinals
    def apply(str: String): String = Normalizer
      .normalize(
        ordinalRegex.replaceAllIn(
          str,
          _.group(0)(0) match
            case 'º' | '°' => "\u0001".toString
            case 'ª'       => '\u0002'.toString
            case '½'       => '\u0003'.toString
        ),
        Normalizer.Form.NFKC
      )
      .replace('\u0001', 'º')
      .replace('\u0002', 'ª')
      .replace('\u0003', '½')

  // https://www.compart.com/en/unicode/block/U+1F300
  // https://www.compart.com/en/unicode/block/U+1F600
  // https://www.compart.com/en/unicode/block/U+1F900
  private val multibyteSymbolsRegex =
    raw"[\p{So}\p{block=Emoticons}\p{block=Miscellaneous Symbols and Pictographs}\p{block=Supplemental Symbols and Pictographs}]".r
  def removeMultibyteSymbols(str: String): String = multibyteSymbolsRegex.replaceAllIn(str, "")

  // for publicly listed text like team names, study names, forum topics...
  def fullCleanUp(str: String) = removeMultibyteSymbols(removeChars(normalize(str), isGarbageChar)).trim

  // for inner text like study chapter names, possibly forum posts and team descriptions
  def softCleanUp(str: String) = removeChars(normalize(str), isInvisibleChar(_)).trim

  def decodeUriPath(input: String): Option[String] =
    try play.utils.UriEncoding.decodePath(input, "UTF-8").some
    catch case _: play.utils.InvalidUriEncodingException => None

  private val onelineR                           = """\s+""".r
  def shorten(text: String, length: Int): String = shorten(text, length, "…")
  def shorten(text: String, length: Int, sep: String): String =
    val oneline = onelineR.replaceAllIn(text, " ")
    if oneline.lengthIs > length + sep.length then oneline.take(length) ++ sep
    else oneline

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

  object base64:
    import java.util.Base64
    import java.nio.charset.StandardCharsets.UTF_8
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes UTF_8)
    def decode(txt: String): Option[String] =
      try Some(new String(Base64.getDecoder decode txt, UTF_8))
      catch case _: java.lang.IllegalArgumentException => none

  val atUsernameRegex    = RawHtml.atUsernameRegex
  val forumPostPathRegex = """(?:(?<= )|^)\b([\w-]+/[\w-]+)\b(?:(?= )|$)""".r

  object html:

    inline def raw(inline html: Html) = scalatags.Text.all.raw(html.value)

    def richText(rawText: String, nl2br: Boolean = true, expandImg: Boolean = true)(using
        config.NetDomain
    ): Frag =
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

    def markdownLinksOrRichText(text: String)(using config.NetDomain): Frag =
      val escaped = Html(escapeHtmlRaw(text))
      val marked  = RawHtml.justMarkdownLinks(escaped)
      if marked == escaped then richText(text)
      else nl2brUnsafe(marked.value)

    def safeJsonValue(jsValue: JsValue): String =
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
            .map { (k, v) =>
              s"${safeJsonString(k)}:${safeJsonValue(v)}"
            }
            .mkString("{", ",", "}")

  def underscoreFen(fen: chess.format.Fen.Epd) = fen.value.replace(" ", "_")
