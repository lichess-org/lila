package lila.common

import lila.core.config.NetDomain

import RawHtml.*

class RawHtmlTest extends munit.FunSuite:

  given NetDomain = NetDomain("lichess.org")
  given munit.Compare[Html, String] with
    def isEqual(obtained: Html, expected: String): Boolean = obtained.value == expected

  val htmlTags = "<[^>]++>".r
  def copyLinkConsistency(text: String) =
    // Plain text of linkified text >> linkify to the same result.
    val firstHtml = addLinks(text)
    val copyText = htmlTags.replaceAllIn(firstHtml.value, "")
    assertEquals(firstHtml, addLinks(copyText))

  test("http external"):
    val url = "http://zombo.com"
    assertEquals(
      addLinks(s"""link to $url here"""),
      s"""link to <a rel="nofollow noreferrer" href="$url" target="_blank">$url</a> here"""
    )
  test("hide https >> text"):
    val url = "zombo.com"
    assertEquals(
      addLinks(s"""link to https://$url here"""),
      s"""link to <a rel="nofollow noreferrer" href="https://$url" target="_blank">$url</a> here"""
    )
  test("default to https"):
    val url = "zombo.com"
    assertEquals(
      addLinks(s"""link to $url here"""),
      s"""link to <a rel="nofollow noreferrer" href="https://$url" target="_blank">$url</a> here"""
    )
  test("skip buggy url like http://foo@bar"):
    val url = "http://foo@bar"
    assert(!addLinks(s"""link to $url here""").value.contains("""href="http://foo""""))
  test("ignore image from untrusted host"):
    val url = "http://zombo.com/pic.jpg"
    assertEquals(
      addLinks(s"""link to $url here"""),
      s"""link to <a rel="nofollow noreferrer" href="$url" target="_blank">$url</a> here"""
    )
  test("detect direct giphy gif URL"):
    val url = "https://media.giphy.com/media/s0mE1d/giphy.gif"
    val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
    assertEquals(
      addLinks(s"""img to $url here"""),
      s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    )
  test("detect indirect without tags giphy gif URL"):
    val url = "https://giphy.com/gifs/s0mE1d"
    val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
    assertEquals(
      addLinks(s"""img to $url here"""),
      s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    )
  test("detect indirect with tags giphy gif URL"):
    val url = "https://giphy.com/gifs/some-text-1-s0mE1d"
    val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
    assertEquals(
      addLinks(s"""img to $url here"""),
      s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    )
  test("detect imgur image URL"):
    val url = "https://imgur.com/NXy19Im"
    val picUrl = "https://i.imgur.com/NXy19Im.jpg"
    assertEquals(
      addLinks(s"""img to $url here"""),
      s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    )
  test("ignore imgur image URL >> quotes"):
    val url = "http://i.imgur.com/Cku31nh.png"
    assertEquals(
      addLinks(s"""img to "$url" here"""),
      s"""img to &quot;<a rel="nofollow noreferrer" href="$url" target="_blank">$url</a>&quot; here"""
    )
  test("ignore imgur gallery URL"):
    val url = "http://imgur.com/gallery/pMtTE"
    assertEquals(
      addLinks(s"""link to $url here"""),
      s"""link to <a rel="nofollow noreferrer" href="$url" target="_blank">$url</a> here"""
    )

  test("internal links"):
    assertEquals(
      addLinks("lichess.org/@/foo/games"),
      """<a href="/@/foo/games">lichess.org/@/foo/games</a>"""
    )
    assertEquals(addLinks("lichess.org/@/foo"), """<a href="/@/foo">@foo</a>""")
    assertEquals(addLinks("http://lichess.org/"), """<a href="/">lichess.org/</a>""")
    assertEquals(addLinks("http://lichess.org"), """<a href="/">lichess.org</a>""")
    assertEquals(addLinks("@foo"), """<a href="/@/foo">@foo</a>""")

  test("handle weird characters"):
    assertEquals(addLinks("lichess.org/-–%20"), """<a href="/-–%20">lichess.org/-–%20</a>""")

  test("handle multiple links"):
    assertEquals(
      addLinks("@foo blah lichess.org"),
      """<a href="/@/foo">@foo</a> blah <a href="/">lichess.org</a>"""
    )
    assertEquals(
      addLinks("b foo.com blah lichess.org"),
      """b <a rel="nofollow noreferrer" href="https://foo.com" target="_blank">foo.com</a> blah <a href="/">lichess.org</a>"""
    )

  test("handle trailing punctuation"):
    assertEquals(addLinks("lichess.org."), """<a href="/">lichess.org</a>.""")
    assertEquals(addLinks("lichess.org)"), """<a href="/">lichess.org</a>)""")
    assertEquals(addLinks("lichess.org/()"), """<a href="/()">lichess.org/()</a>""")

    assertEquals(addLinks("lichess.org/())"), """<a href="/()">lichess.org/()</a>)""")
    assertEquals(addLinks("lichess.org/(2)-)?"), """<a href="/(2)-">lichess.org/(2)-</a>)?""")

    assertEquals(addLinks("lichess.org.-"), """<a href="/">lichess.org</a>.-""")

    assertEquals(addLinks("lichess.org/foo:bar"), """<a href="/foo:bar">lichess.org/foo:bar</a>""")
    assertEquals(addLinks("lichess.org/foo:bar:"), """<a href="/foo:bar">lichess.org/foo:bar</a>:""")

  test("handle embedded links"):
    assertEquals(addLinks(".lichess.org"), """.lichess.org""")
    assertEquals(addLinks("/lichess.org"), """/lichess.org""")
    assertEquals(addLinks(".http://lichess.org"), """.<a href="/">lichess.org</a>""")

    assertEquals(addLinks("/http://lichess.org"), """/<a href="/">lichess.org</a>""")

  test("handle ambig path separator"):
    assertEquals(addLinks("lichess.org#f"), """<a href="/#f">lichess.org/#f</a>""")
    assertEquals(addLinks("lichess.org?f"), """<a href="/?f">lichess.org/?f</a>""")

  test("pass through plain text (fast case)"):
    val noUrl = "blah blah foobar"
    assertEquals(addLinks(noUrl), Html(noUrl))

  test("remove tracking tags"):
    val url = "example.com?UTM_CAMPAIGN=spy&utm_source=4everEVIL"
    val clean = "example.com/"
    assertEquals(
      addLinks(url),
      s"""<a rel="nofollow noreferrer" href="https://$clean" target="_blank">$clean</a>"""
    )

  test("tracking parameters be removed"):
    assertEquals(removeUrlTrackingParameters("example.com?utm_campaign=spy&utm_source=evil"), "example.com")
    assertEquals(
      removeUrlTrackingParameters("example.com?UTM_CAMPAIGN=spy&utm_source=4everEVIL"),
      "example.com"
    )
    assertEquals(
      removeUrlTrackingParameters("example.com?UTM_CAMPAIGN=spy&amp;utm_source=4everEVIL"),
      "example.com"
    )
    assertEquals(removeUrlTrackingParameters("example.com?gclid=spy"), "example.com")
    assertEquals(removeUrlTrackingParameters("example.com?notutm_a=ok"), "example.com?notutm_a=ok")
  test("tracking parameters preserve other params"):
    assertEquals(
      removeUrlTrackingParameters("example.com?foo=42&utm_campaign=spy&bar=yay&utm_source=evil"),
      "example.com?foo=42&bar=yay"
    )

  test("markdown add http links"):
    val md = "[Example](http://example.com)"
    assertEquals(
      justMarkdownLinks(Html(md)),
      Html("""<a rel="nofollow noopener noreferrer" href="http://example.com">Example</a>""")
    )

  test("markdown handle $ >> link content"):
    val md =
      "[$$$ test 9$ prize](https://lichess.org/tournament)"
    assertEquals(
      justMarkdownLinks(Html(md)),
      Html(
        """<a rel="nofollow noopener noreferrer" href="https://lichess.org/tournament">$$$ test 9$ prize</a>"""
      )
    )

  test("markdown only allow safe protocols"):
    val md = Html("A [link](javascript:powned) that is not safe.")
    assertEquals(justMarkdownLinks(md), md)

  test("markdown not add br"):
    assertEquals(justMarkdownLinks(Html("\n")), Html("\n"))

  test("markdown not escape html"):
    assertEquals(justMarkdownLinks(Html("&")), Html("&"))

  test("markdown remove tracking tags"):
    val md = "[Example](http://example.com?utm_campaign=spy&utm_source=evil)"
    assertEquals(
      justMarkdownLinks(Html(md)),
      Html("""<a rel="nofollow noopener noreferrer" href="http://example.com">Example</a>""")
    )

  test("atUser expand valid"):
    assertEquals(expandAtUser("@foo"), List("lichess.org/@/foo"))
    assertEquals(expandAtUser("@2foo"), List("lichess.org/@/2foo"))
    assertEquals(expandAtUser("@foo."), List("lichess.org/@/foo", "."))
    assertEquals(expandAtUser("@foo.com"), List("@foo.com"))

    assertEquals(expandAtUser("@foo./"), List("lichess.org/@/foo", "./"))
    assertEquals(expandAtUser("@foo/games"), List("lichess.org/@/foo", "/games"))

  test("linkConsistency at user links"):
    copyLinkConsistency("http://example.com")
    copyLinkConsistency("https://example.com/@foo")
    copyLinkConsistency("lichess.org/@/foo")
    copyLinkConsistency("lichess.org/@/foo/games")
    copyLinkConsistency("@foo/games")
    copyLinkConsistency("@foo")

  test("nl2br convert windows style newlines into <br>"):
    assertEquals(nl2br("hello\r\nworld"), "hello<br>world")
    assertEquals(nl2br("\r\nworld"), "<br>world")
    assertEquals(nl2br("hello\r\n"), "hello<br>")
    assertEquals(nl2br("hello\r\nworld\r\nagain"), "hello<br>world<br>again")

  test("nl2br convert posix style newlines into <br>"):
    assertEquals(nl2br("hello\nworld"), "hello<br>world")
    assertEquals(nl2br("\nworld"), "<br>world")
    assertEquals(nl2br("hello\n"), "hello<br>")
    assertEquals(nl2br("hello\nworld\nagain"), "hello<br>world<br>again")

  test("nl2br not output more than two consecutive <br> chars"):
    assertEquals(nl2br("\n\n\n\ndef"), "<br><br>def")
    assertEquals(nl2br("abc\n\n\n\n"), "abc<br><br>")
    assertEquals(nl2br("abc\n\n\n\ndef"), "abc<br><br>def")
    assertEquals(nl2br("abc\n\n\n\ndef\n\n\n\nabc\n\n\n\ndef"), "abc<br><br>def<br><br>abc<br><br>def")

    assertEquals(nl2br("\r\n\r\n\r\n\ndef"), "<br><br>def")
    assertEquals(nl2br("abc\r\n\r\n\r\n\r\n"), "abc<br><br>")
    assertEquals(nl2br("abc\r\n\r\n\r\n\r\ndef"), "abc<br><br>def")
    assertEquals(
      nl2br("abc\r\n\r\n\r\n\r\ndef\r\n\r\n\r\n\r\nabc\r\n\r\n\r\n\r\ndef"),
      "abc<br><br>def<br><br>abc<br><br>def"
    )
