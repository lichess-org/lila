package lila.base

import org.specs2.mutable.*

import lila.common.{ Html, config }
import RawHtml._

class RawHtmlTest extends Specification {

  given config.NetDomain = config.NetDomain("lichess.org")

  val htmlTags = "<[^>]++>".r
  def copyLinkConsistency(text: String) = {
    // Plain text of linkified text >> linkify to the same result.
    val firstHtml = addLinks(text)
    val copyText  = htmlTags.replaceAllIn(firstHtml.value, "")
    firstHtml === addLinks(copyText)
  }

  "links" >> {
    "http external" >> {
      val url = "http://zombo.com"
      addLinks(s"""link to $url here""") ===
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here"""
    }
    "hide https >> text" >> {
      val url = "zombo.com"
      addLinks(s"""link to https://$url here""") ===
        s"""link to <a rel="nofollow noopener noreferrer" href="https://$url" target="_blank">$url</a> here"""
    }
    "default to https" >> {
      val url = "zombo.com"
      addLinks(s"""link to $url here""") ===
        s"""link to <a rel="nofollow noopener noreferrer" href="https://$url" target="_blank">$url</a> here"""
    }
    "skip buggy url like http://foo@bar" >> {
      val url = "http://foo@bar"
      addLinks(s"""link to $url here""").value.must(contain("""href="http://foo"""")).not
    }
    "ignore image from untrusted host" >> {
      val url = "http://zombo.com/pic.jpg"
      addLinks(s"""link to $url here""") ===
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here"""
    }
    "detect direct giphy gif URL" >> {
      val url    = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") ===
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect indirect without tags giphy gif URL" >> {
      val url    = "https://giphy.com/gifs/s0mE1d"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") ===
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect indirect with tags giphy gif URL" >> {
      val url    = "https://giphy.com/gifs/some-text-1-s0mE1d"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") ===
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect imgur image URL" >> {
      val url    = "https://imgur.com/NXy19Im"
      val picUrl = "https://i.imgur.com/NXy19Im.jpg"
      addLinks(s"""img to $url here""") ===
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "ignore imgur image URL >> quotes" >> {
      val url = "http://i.imgur.com/Cku31nh.png"
      addLinks(s"""img to "$url" here""") ===
        s"""img to &quot;<a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a>&quot; here"""
    }
    "ignore imgur gallery URL" >> {
      val url = "http://imgur.com/gallery/pMtTE"
      addLinks(s"""link to $url here""") ===
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here"""
    }

    "internal links" >> {
      addLinks("lichess.org/@/foo/games") ===
        """<a href="/@/foo/games">lichess.org/@/foo/games</a>"""
      addLinks("lichess.org/@/foo") === """<a href="/@/foo">@foo</a>"""
      addLinks("http://lichess.org/") === """<a href="/">lichess.org/</a>"""
      addLinks("http://lichess.org") === """<a href="/">lichess.org</a>"""
      addLinks("@foo") === """<a href="/@/foo">@foo</a>"""
    }

    "handle weird characters" >> {
      addLinks("lichess.org/-–%20") === """<a href="/-–%20">lichess.org/-–%20</a>"""
    }

    "handle multiple links" >> {
      addLinks(
        "@foo blah lichess.org"
      ) === """<a href="/@/foo">@foo</a> blah <a href="/">lichess.org</a>"""
      addLinks("b foo.com blah lichess.org") ===
        """b <a rel="nofollow noopener noreferrer" href="https://foo.com" target="_blank">foo.com</a> blah <a href="/">lichess.org</a>"""
    }

    "handle trailing punctuation" >> {
      addLinks("lichess.org.") === """<a href="/">lichess.org</a>."""
      addLinks("lichess.org)") === """<a href="/">lichess.org</a>)"""
      addLinks("lichess.org/()") === """<a href="/()">lichess.org/()</a>"""

      addLinks("lichess.org/())") === """<a href="/()">lichess.org/()</a>)"""
      addLinks("lichess.org/(2)-)?") === """<a href="/(2)-">lichess.org/(2)-</a>)?"""

      addLinks("lichess.org.-") === """<a href="/">lichess.org</a>.-"""

      addLinks("lichess.org/foo:bar") === """<a href="/foo:bar">lichess.org/foo:bar</a>"""
      addLinks("lichess.org/foo:bar:") === """<a href="/foo:bar">lichess.org/foo:bar</a>:"""
    }

    "handle embedded links" >> {
      addLinks(".lichess.org") === """.lichess.org"""
      addLinks("/lichess.org") === """/lichess.org"""
      addLinks(".http://lichess.org") === """.<a href="/">lichess.org</a>"""

      addLinks("/http://lichess.org") === """/<a href="/">lichess.org</a>"""
    }

    "handle ambig path separator" >> {
      addLinks("lichess.org#f") === """<a href="/#f">lichess.org/#f</a>"""
      addLinks("lichess.org?f") === """<a href="/?f">lichess.org/?f</a>"""
    }

    "pass through plain text (fast case)" >> {
      val noUrl = "blah blah foobar"
      addLinks(noUrl) === Html(noUrl)
    }

    "remove tracking tags" >> {
      val url   = "example.com?UTM_CAMPAIGN=spy&utm_source=4everEVIL"
      val clean = "example.com/"
      addLinks(
        url
      ) === s"""<a rel="nofollow noopener noreferrer" href="https://$clean" target="_blank">$clean</a>"""
    }
  }

  "tracking parameters" >> {
    "be removed" >> {
      removeUrlTrackingParameters("example.com?utm_campaign=spy&utm_source=evil") === "example.com"
      removeUrlTrackingParameters("example.com?UTM_CAMPAIGN=spy&utm_source=4everEVIL") === "example.com"
      removeUrlTrackingParameters(
        "example.com?UTM_CAMPAIGN=spy&amp;utm_source=4everEVIL"
      ) === "example.com"
      removeUrlTrackingParameters("example.com?gclid=spy") === "example.com"
      removeUrlTrackingParameters("example.com?notutm_a=ok") === "example.com?notutm_a=ok"
    }
    "preserve other params" >> {
      removeUrlTrackingParameters(
        "example.com?foo=42&utm_campaign=spy&bar=yay&utm_source=evil"
      ) === "example.com?foo=42&bar=yay"
    }
  }

  "markdown links" >> {

    "add http links" >> {
      val md = "[Example](http://example.com)"
      justMarkdownLinks(
        Html(md)
      ) === Html("""<a rel="nofollow noopener noreferrer" href="http://example.com">Example</a>""")
    }

    "handle $ >> link content" >> {
      val md =
        "[$$$ test 9$ prize](https://lichess.org/tournament)"
      justMarkdownLinks(
        Html(md)
      ) === Html(
        """<a rel="nofollow noopener noreferrer" href="https://lichess.org/tournament">$$$ test 9$ prize</a>"""
      )
    }

    "only allow safe protocols" >> {
      val md = Html("A [link](javascript:powned) that is not safe.")
      justMarkdownLinks(md) === md
    }

    "not add br" >> {
      justMarkdownLinks(Html("\n")) === Html("\n")
    }

    "not escape html" >> {
      justMarkdownLinks(Html("&")) === Html("&")
    }

    "remove tracking tags" >> {
      val md = "[Example](http://example.com?utm_campaign=spy&utm_source=evil)"
      justMarkdownLinks(
        Html(md)
      ) === Html("""<a rel="nofollow noopener noreferrer" href="http://example.com">Example</a>""")
    }
  }

  "atUser" >> {
    "expand valid" >> {
      expandAtUser("@foo") === List("lichess.org/@/foo")
      expandAtUser("@2foo") === List("lichess.org/@/2foo")
      expandAtUser("@foo.") === List("lichess.org/@/foo", ".")
      expandAtUser("@foo.com") === List("@foo.com")

      expandAtUser("@foo./") === List("lichess.org/@/foo", "./")
      expandAtUser("@foo/games") === List("lichess.org/@/foo", "/games")
    }
  }

  "linkConsistency" >> {
    "at user links" >> {
      copyLinkConsistency("http://example.com")
      copyLinkConsistency("https://example.com/@foo")
      copyLinkConsistency("lichess.org/@/foo")
      copyLinkConsistency("lichess.org/@/foo/games")
      copyLinkConsistency("@foo/games")
      copyLinkConsistency("@foo")
    }
  }

  "nl2br" >> {
    "convert windows style newlines into <br>" >> {
      nl2br("hello\r\nworld") === "hello<br>world"
      nl2br("\r\nworld") === "<br>world"
      nl2br("hello\r\n") === "hello<br>"
      nl2br("hello\r\nworld\r\nagain") === "hello<br>world<br>again"
    }

    "convert posix style newlines into <br>" >> {
      nl2br("hello\nworld") === "hello<br>world"
      nl2br("\nworld") === "<br>world"
      nl2br("hello\n") === "hello<br>"
      nl2br("hello\nworld\nagain") === "hello<br>world<br>again"
    }

    "not output more than two consecutive <br> chars" >> {
      nl2br("\n\n\n\ndef") === "<br><br>def"
      nl2br("abc\n\n\n\n") === "abc<br><br>"
      nl2br("abc\n\n\n\ndef") === "abc<br><br>def"
      nl2br("abc\n\n\n\ndef\n\n\n\nabc\n\n\n\ndef") === "abc<br><br>def<br><br>abc<br><br>def"

      nl2br("\r\n\r\n\r\n\ndef") === "<br><br>def"
      nl2br("abc\r\n\r\n\r\n\r\n") === "abc<br><br>"
      nl2br("abc\r\n\r\n\r\n\r\ndef") === "abc<br><br>def"
      nl2br(
        "abc\r\n\r\n\r\n\r\ndef\r\n\r\n\r\n\r\nabc\r\n\r\n\r\n\r\ndef"
      ) === "abc<br><br>def<br><br>abc<br><br>def"
    }
  }
}
