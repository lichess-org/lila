package lila.common

import scalalib.StringUtils.{ escapeHtmlRaw, escapeHtmlRawInPlace }

import java.lang.Character.isLetterOrDigit
import java.lang.{ Math, StringBuilder as jStringBuilder }
import java.util.regex.Matcher
import scala.annotation.{ switch, tailrec }

import lila.common.Html
import lila.core.config.NetDomain
import lila.core.misc.lpv.LinkRender

object RawHtml:

  def nl2br(s: String): Html =
    val sb = jStringBuilder(s.length)
    var counter = 0
    for char <- s do
      if char == '\n' then
        counter += 1
        if counter < 3 then sb.append("<br>")
      else if char != '\r' then
        counter = 0
        sb.append(char)
    Html(sb.toString)

  private val urlPattern = (
    """(?i)\b[a-z](?>""" + // pull out first char for perf.
      """ttp(?<=http)s?://(\w[-\w.~!$&';=:@]{0,100})|""" + // http(s) links
      """(?<![/@.-].)(?:\w{1,15}+\.){1,3}(?>com|org|edu))""" + // "lichess.org", etc
      """([/?#][-–—\w/.~!$&'()*+,;=:#?@%]{0,300}+)?""" + // path, params
      """(?![\w/~$&*+=#@%])""" // neg lookahead
  ).r.pattern

  private val USER_LINK = """/@/([\w-]{2,30}+)?""".r

  // Matches a lichess username with an '@' prefix if it is used as a single
  // word (i.e. preceded and followed by space or appropriate punctuation):
  // Yes: everyone says @ornicar is a pretty cool guy
  // No: contact@lichess.org, @1, http://example.com/@happy0, @lichess.org
  val atUsernameRegex = """@(?<![\w@#/\[]@)([\w-]{2,30}+)(?![@\w-]|\.\w)""".r

  private val atUsernamePat = atUsernameRegex.pattern

  def expandAtUser(text: String)(using netDomain: NetDomain): List[String] =
    val m = atUsernamePat.matcher(text)
    if m.find then
      var idx = 0
      val buf = List.newBuilder[String]
      while
        if idx < m.start then buf += text.substring(idx, m.start)
        buf += s"${netDomain}/@/${m.group(1)}"
        idx = m.end
        m.find
      do ()
      if idx < text.length then buf += text.substring(idx)
      buf.result()
    else List(text)

  def hasLinks(text: String) = urlPattern.matcher(text).find

  def addLinks(
      text: String,
      expandImg: Boolean = true,
      linkRender: Option[LinkRender] = None
  )(using netDomain: NetDomain): Html =
    expandAtUser(text).map { expanded =>
      val m = urlPattern.matcher(expanded)

      if !m.find then escapeHtmlRaw(expanded) // preserve fast case!
      else
        val sb = new jStringBuilder(expanded.length + 200)
        val sArr = expanded.toCharArray
        var lastAppendIdx = 0

        while
          val start = m.start
          escapeHtmlRawInPlace(sb, sArr, lastAppendIdx, start)

          val domainS = Math.max(m.start(1), start)
          val pathS = m.start(2)

          val end =
            val e = m.end
            if isLetterOrDigit(sArr(e - 1)) then e
            else adjustUrlEnd(sArr, Math.max(pathS, domainS), e)

          val domain = expanded.substring(
            domainS,
            pathS match
              case -1 => end
              case _ => pathS
          )

          val isTldInternal = netDomain.value == domain

          val csb = new jStringBuilder()
          if !isTldInternal then csb.append(domain)
          if pathS >= 0 then
            if sArr(pathS) != '/' then csb.append('/')
            csb.append(sArr, pathS, end - pathS)

          val allButScheme = escapeHtmlRaw(removeUrlTrackingParameters(csb.toString))
          lazy val isHttp = domainS - start == 7
          lazy val url = (if isHttp then "http://" else "https://") + allButScheme
          lazy val text = if isHttp then url else allButScheme

          sb.append:
            if isTldInternal then
              linkRender
                .flatMap { _(allButScheme, text).map(_.render) }
                .getOrElse(s"""<a href="${
                    if allButScheme.isEmpty then "/"
                    else allButScheme
                  }">${allButScheme match
                    case USER_LINK(user) => "@" + user
                    case _ => s"${netDomain}$allButScheme"
                  }</a>""")
            else
              {
                if (end < sArr.length && sArr(end) == '"') || !expandImg then None
                else imgUrl(url)
              }.getOrElse:
                s"""<a rel="nofollow noreferrer" href="$url" target="_blank">$text</a>"""

          lastAppendIdx = end
          m.find
        do ()

        escapeHtmlRawInPlace(sb, sArr, lastAppendIdx, sArr.length)
        sb.toString
    } match
      case one :: Nil => Html(one)
      case many => Html(many.mkString(""))

  private def adjustUrlEnd(sArr: Array[Char], start: Int, end: Int): Int =
    var last = end - 1
    while (sArr(last): @switch) match
        case '.' | ',' | '?' | '!' | ':' | ';' | '–' | '—' | '@' | '\'' | '(' => true
        case _ => false
    do last -= 1

    if sArr(last) == ')' then
      @tailrec def pCnter(idx: Int, acc: Int): Int =
        if idx >= last then acc
        else
          pCnter(
            idx + 1,
            acc + (sArr(idx) match
              case '(' => 1
              case ')' => -1
              case _ => 0)
          )
      var parenCnt = pCnter(start, -1)
      while (sArr(last): @switch).match
          case '.' | ',' | '?' | '!' | ':' | ';' | '–' | '—' | '@' | '\'' => true
          case '(' => parenCnt -= 1; true
          case ')' => parenCnt += 1; parenCnt <= 0
          case _ => false
      do last -= 1
    last + 1

  private val imgurRegex = """https?://(?:i\.)?imgur\.com/(\w++)(?:\.jpe?g|\.png|\.gif)?""".r
  private val giphyRegex =
    """https://(?:media\.giphy\.com/media/|giphy\.com/gifs/(?:\w+-)*+)(\w+)(?:/giphy\.gif)?""".r
  private val postimgRegex = """https://(?:i\.)?postimg\.cc/([\w/-]+)(?:\.jpe?g|\.png|\.gif)?""".r

  private def imgUrl(url: String): Option[Html] =
    url
      .match
        case imgurRegex(id) => Some(s"""https://i.imgur.com/$id.jpg""")
        case giphyRegex(id) => Some(s"""https://media.giphy.com/media/$id/giphy.gif""")
        case postimgRegex(id) => Some(s"""https://i.postimg.cc/$id.jpg""")
        case _ => None
      .map { img =>
        Html(s"""<img class="embed" src="$img" alt="$url"/>""")
      }

  private val markdownLinkRegex = """\[([^]]++)\]\((https?://[^)]++)\)""".r
  def justMarkdownLinks(escapedHtml: Html): Html = Html:
    markdownLinkRegex.replaceAllIn(
      escapedHtml.value,
      m =>
        val content = Matcher.quoteReplacement(m.group(1))
        val href = removeUrlTrackingParameters(m.group(2))
        s"""<a rel="nofollow noopener noreferrer" href="$href">$content</a>"""
    )

  private val trackingParametersRegex =
    """(?i)(?:\?|&(?:amp;)?)(?:utm\\?_\w+|gclid|gclsrc|\\?_ga)=\w+""".r
  def removeUrlTrackingParameters(url: String): String =
    trackingParametersRegex.replaceAllIn(url, "")
