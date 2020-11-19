package lila.base

import org.specs2.mutable.Specification
// import scalatags.Text.all._

import RawHtml._

class RawHtmlTest extends Specification {

  val htmlTags = "<[^>]++>".r
  def copyLinkConsistency(text: String) = {
    // Plain text of linkified text should linkify to the same result.
    val firstHtml = addLinks(text)
    val copyText  = htmlTags.replaceAllIn(firstHtml, "")
    firstHtml must_== addLinks(copyText)
  }

  "links" should {
    "http external" in {
      val url = "http://zombo.com"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here"""
    }
    "hide https in text" in {
      val url = "zombo.com"
      addLinks(s"""link to https://$url here""") must_==
        s"""link to <a rel="nofollow noopener noreferrer" href="https://$url" target="_blank">$url</a> here"""
    }
    "default to https" in {
      val url = "zombo.com"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow noopener noreferrer" href="https://$url" target="_blank">$url</a> here"""
    }
    "skip buggy url like http://foo@bar" in {
      val url = "http://foo@bar"
      addLinks(s"""link to $url here""") must not contain """href="http://foo""""
    }
    "ignore image from untrusted host" in {
      val url = "http://zombo.com/pic.jpg"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here"""
    }
    "detect direct giphy gif URL" in {
      val url    = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect indirect without tags giphy gif URL" in {
      val url    = "https://giphy.com/gifs/s0mE1d"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect indirect with tags giphy gif URL" in {
      val url    = "https://giphy.com/gifs/some-text-1-s0mE1d"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect imgur image URL" in {
      val url    = "https://imgur.com/NXy19Im"
      val picUrl = "https://i.imgur.com/NXy19Im.jpg"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "ignore imgur image URL in quotes" in {
      val url = "http://i.imgur.com/Cku31nh.png"
      addLinks(s"""img to "$url" here""") must_==
        s"""img to &quot;<a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a>&quot; here"""
    }
    "ignore imgur gallery URL" in {
      val url = "http://imgur.com/gallery/pMtTE"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here"""
    }

    "internal links" in {
      addLinks("lishogi.org/@/foo/games") must_==
        """<a href="/@/foo/games">lishogi.org/@/foo/games</a>"""
      addLinks("lishogi.org/@/foo") must_== """<a href="/@/foo">@foo</a>"""
      addLinks("http://lishogi.org/") must_== """<a href="/">lishogi.org/</a>"""
      addLinks("http://lishogi.org") must_== """<a href="/">lishogi.org</a>"""
      addLinks("@foo") must_== """<a href="/@/foo">@foo</a>"""
    }

    "handle weird characters" in {
      addLinks("lishogi.org/-–%20") must_== """<a href="/-–%20">lishogi.org/-–%20</a>"""
    }

    "handle multiple links" in {
      addLinks(
        "@foo blah lishogi.org"
      ) must_== """<a href="/@/foo">@foo</a> blah <a href="/">lishogi.org</a>"""
      addLinks("b foo.com blah lishogi.org") must_==
        """b <a rel="nofollow noopener noreferrer" href="https://foo.com" target="_blank">foo.com</a> blah <a href="/">lishogi.org</a>"""
    }

    "handle trailing punctuation" in {
      addLinks("lishogi.org.") must_== """<a href="/">lishogi.org</a>."""
      addLinks("lishogi.org)") must_== """<a href="/">lishogi.org</a>)"""
      addLinks("lishogi.org/()") must_== """<a href="/()">lishogi.org/()</a>"""

      addLinks("lishogi.org/())") must_== """<a href="/()">lishogi.org/()</a>)"""
      addLinks("lishogi.org/(2)-)?") must_== """<a href="/(2)-">lishogi.org/(2)-</a>)?"""

      addLinks("lishogi.org.-") must_== """<a href="/">lishogi.org</a>.-"""

      addLinks("lishogi.org/foo:bar") must_== """<a href="/foo:bar">lishogi.org/foo:bar</a>"""
      addLinks("lishogi.org/foo:bar:") must_== """<a href="/foo:bar">lishogi.org/foo:bar</a>:"""
    }

    "handle embedded links" in {
      addLinks(".lishogi.org") must_== """.lishogi.org"""
      addLinks("/lishogi.org") must_== """/lishogi.org"""
      addLinks(".http://lishogi.org") must_== """.<a href="/">lishogi.org</a>"""

      addLinks("/http://lishogi.org") must_== """/<a href="/">lishogi.org</a>"""
    }

    "handle ambig path separator" in {
      addLinks("lishogi.org#f") must_== """<a href="/#f">lishogi.org/#f</a>"""
      addLinks("lishogi.org?f") must_== """<a href="/?f">lishogi.org/?f</a>"""
    }

    "pass through plain text (fast case)" in {
      val noUrl = "blah blah foobar"
      addLinks(noUrl) must_== noUrl  // eq
      addLinks(noUrl) must be(noUrl) // instance eq - fails in scala 2.13
    }
  }

  "markdown links" should {
    "add http links" in {
      val md = "[Example](http://example.com)"
      markdownLinks(md) must_== """<a href="http://example.com">Example</a>"""
    }

    "only allow safe protocols" in {
      val md = "A [link](javascript:powned) that is not safe."
      markdownLinks(md) must_== md
    }

    "addBr" in {
      markdownLinks("\n") must_== "<br>"
    }

    "escape html" in {
      markdownLinks("&") must_== "&amp;"
    }
  }

  "atUser" should {
    "expand valid" in {
      expandAtUser("@foo") must_== List("lishogi.org/@/foo")
      expandAtUser("@2foo") must_== List("lishogi.org/@/2foo")
      expandAtUser("@foo.") must_== List("lishogi.org/@/foo", ".")
      expandAtUser("@foo.com") must_== List("@foo.com")

      expandAtUser("@foo./") must_== List("lishogi.org/@/foo", "./")
      expandAtUser("@foo/games") must_== List("lishogi.org/@/foo", "/games")
    }
  }

  "linkConsistency" should {
    "at user links" in {
      copyLinkConsistency("http://example.com")
      copyLinkConsistency("https://example.com/@foo")
      copyLinkConsistency("lishogi.org/@/foo")
      copyLinkConsistency("lishogi.org/@/foo/games")
      copyLinkConsistency("@foo/games")
      copyLinkConsistency("@foo")
    }
  }

  "nl2br" should {
    "convert windows style newlines into <br>" in {
      nl2br("hello\r\nworld") must_== "hello<br>world"
      nl2br("\r\nworld") must_== "<br>world"
      nl2br("hello\r\n") must_== "hello<br>"
      nl2br("hello\r\nworld\r\nagain") must_== "hello<br>world<br>again"
    }

    "convert posix style newlines into <br>" in {
      nl2br("hello\nworld") must_== "hello<br>world"
      nl2br("\nworld") must_== "<br>world"
      nl2br("hello\n") must_== "hello<br>"
      nl2br("hello\nworld\nagain") must_== "hello<br>world<br>again"
    }

    "not output more than two consecutive <br> chars" in {
      nl2br("\n\n\n\ndef") must_== "<br><br>def"
      nl2br("abc\n\n\n\n") must_== "abc<br><br>"
      nl2br("abc\n\n\n\ndef") must_== "abc<br><br>def"
      nl2br("abc\n\n\n\ndef\n\n\n\nabc\n\n\n\ndef") must_== "abc<br><br>def<br><br>abc<br><br>def"

      nl2br("\r\n\r\n\r\n\ndef") must_== "<br><br>def"
      nl2br("abc\r\n\r\n\r\n\r\n") must_== "abc<br><br>"
      nl2br("abc\r\n\r\n\r\n\r\ndef") must_== "abc<br><br>def"
      nl2br(
        "abc\r\n\r\n\r\n\r\ndef\r\n\r\n\r\n\r\nabc\r\n\r\n\r\n\r\ndef"
      ) must_== "abc<br><br>def<br><br>abc<br><br>def"
    }
  }
}
