package lila.common

import scala.annotation.{ tailrec, switch }
import java.text.Normalizer
import java.lang.{ StringBuilder => jStringBuilder, Math }
import play.api.libs.json._
import play.twirl.api.Html

import lila.common.base.StringUtils.{ safeJsonString, escapeHtml => escapeHtmlRaw }

object String {

  val erased = "<deleted>"
  val erasedHtml = Html("&lt;deleted&gt;")

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

  // Matches a lichess username with an '@' prefix if it is used as a single
  // word (i.e. preceded and followed by space or appropriate punctuation):
  // Yes: everyone says @ornicar is a pretty cool guy
  // No: contact@lichess.org, @1, http://example.com/@happy0, @lichess.org
  // TODO(isaac): support @user/<subpath> syntax
  val atUsernameRegex = """@(?<![\w@#/]@)([\w-]{2,30}+)(?![@\w-]|\.\w)""".r

  object html {

    def nl2brUnsafe(s: String) = Html {
      var i = s.indexOf('\n')
      if (i < 0) s
      else {
        val sb = new jStringBuilder(s.length + 30)
        var copyIdx = 0
        do {
          if (i > copyIdx) {
            // copyIdx >= 0, so i - 1 >= 0
            sb.append(s, copyIdx, if (s.charAt(i - 1) == '\r') i - 1 else i)
          }
          sb.append("<br />")
          copyIdx = i + 1
          i = s.indexOf('\n', copyIdx)
        } while (i >= 0)

        sb.append(s, copyIdx, s.length)
        sb.toString
      }
    }

    def nl2br(text: String): Html = nl2brUnsafe(escapeHtmlRaw(text))

    def richText(rawText: String, nl2br: Boolean = true) = {
      val withLinks = addLinksRaw(rawText)
      if (nl2br) nl2brUnsafe(withLinks) else Html(withLinks)
    }

    private[this] val urlPattern = (
      """(?i)\b[a-z](?>""" + // pull out first char for perf.
      """ttp(?<=http)s?://(\w[-\w.~!$&';=:@]{0,100})|""" + // http(s) links
      """(?<![/@.-].)(?:\w{1,15}+\.){1,3}(?>com|org|edu))""" + // "lichess.org", etc
      """([/?#][-–—\w/.~!$&'()*+,;=:#?@]{0,300}+)?""" + // path, params
      """(?![\w/~$&*+=#@])""" // neg lookahead
    ).r.pattern

    private[this] val USER_LINK = """/@/([\w-]{2,30}+)?""".r

    private[this] final val DOMAIN = "lichess.org"
    private[this] final val linkReplace = DOMAIN + "/@/$1"

    private[common] def expandAtUserRaw(text: String): List[String] = {
      val m = atUsernameRegex.pattern.matcher(text)
      if (m.find) {
        var idx = 0
        val buf = List.newBuilder[String]
        do {
          if (idx < m.start) buf += text.substring(idx, m.start)
          buf += DOMAIN + "/@/" + m.group(1)
          idx = m.end
        } while (m.find)
        if (idx < text.length) buf += text.substring(idx)
        buf.result
      } else List(text)
    }

    private[common] def addLinksRaw(text: String): String = {
      expandAtUserRaw(text) map { expanded =>
        val m = urlPattern.matcher(expanded)

        if (!m.find) escapeHtmlRaw(expanded) // preserve fast case!
        else {
          val sb = new jStringBuilder(expanded.length + 200)
          val sArr = expanded.toCharArray
          var lastAppendIdx = m.start
          escapeHtmlRaw(sb, sArr, 0, lastAppendIdx)

          do {
            val start = m.start
            val domainS = Math.max(m.start(1), start)
            val pathS = m.start(2)

            val end = {
              val e = m.end
              if (sArr(e - 1).isLetterOrDigit) e
              else adjustUrlEnd(sArr, Math.max(pathS, domainS), e)
            }

            val domain = expanded.substring(domainS, pathS match {
              case -1 => end
              case _ => pathS
            })

            val isTldInternal = DOMAIN == domain

            val csb = new jStringBuilder()
            if (!isTldInternal) csb.append(domain)
            if (pathS >= 0) {
              if (sArr(pathS) != '/') csb.append('/')
              csb.append(sArr, pathS, end - pathS)
            }

            val allButScheme = escapeHtmlRaw(csb.toString)

            if (isTldInternal) {
              sb.append(s"""<a href="${
                if (allButScheme.isEmpty) "/"
                else allButScheme
              }">${
                allButScheme match {
                  case USER_LINK(user) => "@" + user
                  case _ => DOMAIN + allButScheme
                }
              }</a>""")
            } else {
              val isHttp = domainS - start == 7
              val url = (if (isHttp) "http://" else "https://") + allButScheme
              val text = if (isHttp) url else allButScheme
              sb.append(s"""<a rel="nofollow" href="$url" target="_blank">${
                imgUrl(url).getOrElse(text)
              }</a>""")
            }
            lastAppendIdx = end
          } while (m.find)

          escapeHtmlRaw(sb, sArr, lastAppendIdx, sArr.length)
          sb
        }
      } concat
    }

    private[this] def adjustUrlEnd(sArr: Array[Char], start: Int, end: Int): Int = {
      var last = end - 1
      while ((sArr(last): @switch) match {
        case '.' | ',' | '?' | '!' | ';' | '-' | '–' | '—' | '@' | '\'' | '(' => true
        case _ => false
      }) { last -= 1 }

      if (sArr(last) == ')') {
        @tailrec def pCnter(idx: Int, acc: Int): Int =
          if (idx >= last) acc
          else pCnter(idx + 1, acc + (sArr(idx) match {
            case '(' => 1
            case ')' => -1
            case _ => 0
          }))
        var parenCnt = pCnter(start, -1)
        while ((sArr(last): @switch) match {
          case '.' | ',' | '?' | '!' | ';' | '-' | '–' | '—' | '@' | '\'' => true
          case '(' => { parenCnt -= 1; true }
          case ')' => { parenCnt += 1; parenCnt <= 0 }
          case _ => false
        }) { last -= 1 }
      }
      last + 1
    }

    private[this] val imgurRegex = """https?://imgur\.com/(\w+)""".r
    private[this] val imgUrlRegex = """\.(?:jpg|jpeg|png|gif)$""".r

    private[this] def imgUrl(url: String): Option[String] = (url match {
      case imgurRegex(id) => s"""https://i.imgur.com/$id.jpg""".some
      case _ if imgUrlRegex.find(url) => url.some
      case _ => None
    }) map { img => s"""<img class="embed" src="$img"/>""" }

    private[this] val markdownLinkRegex = """\[([^]]++)\]\((https?://[^)]++)\)""".r

    def markdownLinks(text: String): Html = nl2brUnsafe {
      markdownLinkRegex.replaceAllIn(escapeHtmlRaw(text), """<a href="$2">$1</a>""")
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

    def safeJson(jsValue: JsValue): Html = Html(safeJsonValue(jsValue))

    def escapeHtml(s: String): Html = Html(escapeHtmlRaw(s))
  }
}
