package lila.base

import java.lang.Character.isLetterOrDigit
import java.lang.{ StringBuilder => jStringBuilder, Math }
import scala.annotation.{ switch, tailrec }

import lila.common.base.StringUtils.escapeHtmlRaw

final object RawHtml {

  def nl2br(s: String): String = {
    val sb      = new jStringBuilder(s.length)
    var counter = 0
    for (char <- s) {
      if (char == '\n') {
        counter += 1
        if (counter < 3) {
          sb.append("<br>")
        }
      } else if (char != '\r') {
        counter = 0
        sb.append(char)
      }
    }
    sb.toString
  }

  private[this] val urlPattern = (
    """(?i)\b[a-z](?>""" +                                     // pull out first char for perf.
      """ttp(?<=http)s?://(\w[-\w.~!$&';=:@]{0,100})|""" +     // http(s) links
      """(?<![/@.-].)(?:\w{1,15}+\.){1,3}(?>com|org|edu))""" + // "lichess.org", etc
      """([/?#][-–—\w/.~!$&'()*+,;=:#?@%]{0,300}+)?""" +       // path, params
      """(?![\w/~$&*+=#@%])"""                                 // neg lookahead
  ).r.pattern

  private[this] val USER_LINK = """/@/([\w-]{2,30}+)?""".r

  final private[this] val DOMAIN      = "lichess.org"
  final private[this] val linkReplace = DOMAIN + "/@/$1"

  // Matches a lichess username with an '@' prefix if it is used as a single
  // word (i.e. preceded and followed by space or appropriate punctuation):
  // Yes: everyone says @ornicar is a pretty cool guy
  // No: contact@lichess.org, @1, http://example.com/@happy0, @lichess.org
  // TODO(isaac): support @user/<subpath> syntax
  val atUsernameRegex = """@(?<![\w@#/]@)([\w-]{2,30}+)(?![@\w-]|\.\w)""".r

  private[this] val atUsernamePat = atUsernameRegex.pattern

  def expandAtUser(text: String): List[String] = {
    val m = atUsernamePat.matcher(text)
    if (m.find) {
      var idx = 0
      val buf = List.newBuilder[String]
      do {
        if (idx < m.start) buf += text.substring(idx, m.start)
        buf += DOMAIN + "/@/" + m.group(1)
        idx = m.end
      } while (m.find)
      if (idx < text.length) buf += text.substring(idx)
      buf.result()
    } else List(text)
  }

  def hasLinks(text: String) = urlPattern.matcher(text).find

  def addLinks(text: String, expandImg: Boolean = true): String =
    expandAtUser(text).map { expanded =>
      val m = urlPattern.matcher(expanded)

      if (!m.find) escapeHtmlRaw(expanded) // preserve fast case!
      else {
        val sb            = new jStringBuilder(expanded.length + 200)
        val sArr          = expanded.toCharArray
        var lastAppendIdx = 0

        do {
          val start = m.start
          escapeHtmlRaw(sb, sArr, lastAppendIdx, start)

          val domainS = Math.max(m.start(1), start)
          val pathS   = m.start(2)

          val end = {
            val e = m.end
            if (isLetterOrDigit(sArr(e - 1))) e
            else adjustUrlEnd(sArr, Math.max(pathS, domainS), e)
          }

          val domain = expanded.substring(
            domainS,
            pathS match {
              case -1 => end
              case _  => pathS
            }
          )

          val isTldInternal = DOMAIN == domain

          val csb = new jStringBuilder()
          if (!isTldInternal) csb.append(domain)
          if (pathS >= 0) {
            if (sArr(pathS) != '/') csb.append('/')
            csb.append(sArr, pathS, end - pathS)
          }

          val allButScheme = escapeHtmlRaw(csb.toString)

          if (isTldInternal) {
            sb.append(s"""<a href="${if (allButScheme.isEmpty) "/"
            else allButScheme}">${allButScheme match {
              case USER_LINK(user) => "@" + user
              case _               => DOMAIN + allButScheme
            }}</a>""")
          } else {
            val isHttp = domainS - start == 7
            val url    = (if (isHttp) "http://" else "https://") + allButScheme
            val text   = if (isHttp) url else allButScheme
            val imgHtml = {
              if ((end < sArr.length && sArr(end) == '"') || !expandImg) None
              else imgUrl(url)
            }
            sb.append(
              imgHtml.getOrElse(
                s"""<a rel="nofollow noopener noreferrer" href="$url" target="_blank">$text</a>"""
              )
            )
          }
          lastAppendIdx = end
        } while (m.find)

        escapeHtmlRaw(sb, sArr, lastAppendIdx, sArr.length)
        sb.toString
      }
    } match {
      case one :: Nil => one
      case many       => many mkString ""
    }

  private[this] def adjustUrlEnd(sArr: Array[Char], start: Int, end: Int): Int = {
    var last = end - 1
    while (
      (sArr(last): @switch) match {
        case '.' | ',' | '?' | '!' | ':' | ';' | '–' | '—' | '@' | '\'' | '(' => true
        case _                                                                => false
      }
    ) { last -= 1 }

    if (sArr(last) == ')') {
      @tailrec def pCnter(idx: Int, acc: Int): Int =
        if (idx >= last) acc
        else
          pCnter(
            idx + 1,
            acc + (sArr(idx) match {
              case '(' => 1
              case ')' => -1
              case _   => 0
            })
          )
      var parenCnt = pCnter(start, -1)
      while (
        (sArr(last): @switch) match {
          case '.' | ',' | '?' | '!' | ':' | ';' | '–' | '—' | '@' | '\'' => true
          case '('                                                        => parenCnt -= 1; true
          case ')'                                                        => parenCnt += 1; parenCnt <= 0
          case _                                                          => false
        }
      ) { last -= 1 }
    }
    last + 1
  }

  private[this] val imgurRegex = """https?://(?:i\.)?imgur\.com/(\w+)(?:\.jpe?g|\.png|\.gif)?""".r
  private[this] val giphyRegex =
    """https://(?:media\.giphy\.com/media/|giphy\.com/gifs/(?:\w+-)*)(\w+)(?:/giphy\.gif)?""".r

  private[this] def imgUrl(url: String): Option[String] =
    (url match {
      case imgurRegex(id) => Some(s"""https://i.imgur.com/$id.jpg""")
      case giphyRegex(id) => Some(s"""https://media.giphy.com/media/$id/giphy.gif""")
      case _              => None
    }) map { img =>
      s"""<img class="embed" src="$img" alt="$url"/>"""
    }

  private[this] val markdownLinkRegex = """\[([^]]++)\]\((https?://[^)]++)\)""".r

  def justMarkdownLinks(escapedHtml: String): String =
    markdownLinkRegex.replaceAllIn(escapedHtml, """<a href="$2">$1</a>""")
}
