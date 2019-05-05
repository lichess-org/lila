package lila.base

import org.specs2.mutable.Specification
// import scalatags.Text.all._

import RawHtml._

class RawHtmlTest extends Specification {

  val htmlTags = "<[^>]++>".r
  def copyLinkConsistency(text: String) = {
    // Plain text of linkified text should linkify to the same result.
    val firstHtml = addLinks(text)
    val copyText = htmlTags.replaceAllIn(firstHtml, "")
    firstHtml must_== addLinks(copyText)
  }

  "links" should {
    "http external" in {
      val url = "http://zombo.com"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
    }
    "hide https in text" in {
      val url = "zombo.com"
      addLinks(s"""link to https://$url here""") must_==
        s"""link to <a rel="nofollow" href="https://$url" target="_blank">$url</a> here"""
    }
    "default to https" in {
      val url = "zombo.com"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="https://$url" target="_blank">$url</a> here"""
    }
    "skip buggy url like http://foo@bar" in {
      val url = "http://foo@bar"
      addLinks(s"""link to $url here""") must not contain ("""href="http://foo"""")
    }
    "ignore image from untrusted host" in {
      val url = "http://zombo.com/pic.jpg"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
    }
    "detect direct giphy gif URL" in {
      val url = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect indirect without tags giphy gif URL" in {
      val url = "https://giphy.com/gifs/s0mE1d"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect indirect with tags giphy gif URL" in {
      val url = "https://giphy.com/gifs/some-text-1-s0mE1d"
      val picUrl = "https://media.giphy.com/media/s0mE1d/giphy.gif"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "detect imgur image URL" in {
      val url = "https://imgur.com/NXy19Im"
      val picUrl = "https://i.imgur.com/NXy19Im.jpg"
      addLinks(s"""img to $url here""") must_==
        s"""img to <img class="embed" src="$picUrl" alt="$url"/> here"""
    }
    "ignore imgur image URL in quotes" in {
      val url = "http://i.imgur.com/Cku31nh.png"
      addLinks(s"""img to "$url" here""") must_==
        s"""img to &quot;<a rel="nofollow" href="$url" target="_blank">$url</a>&quot; here"""
    }
    "ignore imgur gallery URL" in {
      val url = "http://imgur.com/gallery/pMtTE"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
    }

    "internal links" in {
      addLinks("lichess.org/@/foo/games") must_==
        """<a href="/@/foo/games">lichess.org/@/foo/games</a>"""
      addLinks("lichess.org/@/foo") must_== """<a href="/@/foo">@foo</a>"""
      addLinks("http://lichess.org/") must_== """<a href="/">lichess.org/</a>"""
      addLinks("http://lichess.org") must_== """<a href="/">lichess.org</a>"""
      addLinks("@foo") must_== """<a href="/@/foo">@foo</a>"""
    }

    "handle weird characters" in {
      addLinks("lichess.org/-–%20") must_== """<a href="/-–%20">lichess.org/-–%20</a>"""
    }

    "handle multiple links" in {
      addLinks("@foo blah lichess.org") must_== """<a href="/@/foo">@foo</a> blah <a href="/">lichess.org</a>"""
      addLinks("b foo.com blah lichess.org") must_==
        """b <a rel="nofollow" href="https://foo.com" target="_blank">foo.com</a> blah <a href="/">lichess.org</a>"""
    }

    "handle trailing punctuation" in {
      addLinks("lichess.org.") must_== """<a href="/">lichess.org</a>."""
      addLinks("lichess.org)") must_== """<a href="/">lichess.org</a>)"""
      addLinks("lichess.org/()") must_== """<a href="/()">lichess.org/()</a>"""

      addLinks("lichess.org/())") must_== """<a href="/()">lichess.org/()</a>)"""
      addLinks("lichess.org/(2)-)?") must_== """<a href="/(2)">lichess.org/(2)</a>-)?"""

      addLinks("lichess.org.-") must_== """<a href="/">lichess.org</a>.-"""

      addLinks("lichess.org/foo:bar") must_== """<a href="/foo:bar">lichess.org/foo:bar</a>"""
      addLinks("lichess.org/foo:bar:") must_== """<a href="/foo:bar">lichess.org/foo:bar</a>:"""
    }

    "handle embedded links" in {
      addLinks(".lichess.org") must_== """.lichess.org"""
      addLinks("/lichess.org") must_== """/lichess.org"""
      addLinks(".http://lichess.org") must_== """.<a href="/">lichess.org</a>"""

      addLinks("/http://lichess.org") must_== """/<a href="/">lichess.org</a>"""
    }

    "handle ambig path separator" in {
      addLinks("lichess.org#f") must_== """<a href="/#f">lichess.org/#f</a>"""
      addLinks("lichess.org?f") must_== """<a href="/?f">lichess.org/?f</a>"""
    }

    "pass through plain text (fast case)" in {
      val noUrl = "blah blah foobar"
      addLinks(noUrl) must be(noUrl) // instance eq
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
      markdownLinks("\n") must_== "<br />"
    }

    "escape html" in {
      markdownLinks("&") must_== "&amp;"
    }
  }

  "atUser" should {
    "expand valid" in {
      expandAtUser("@foo") must_== List("lichess.org/@/foo")
      expandAtUser("@2foo") must_== List("lichess.org/@/2foo")
      expandAtUser("@foo.") must_== List("lichess.org/@/foo", ".")
      expandAtUser("@foo.com") must_== List("@foo.com")

      expandAtUser("@foo./") must_== List("lichess.org/@/foo", "./")
      expandAtUser("@foo/games") must_== List("lichess.org/@/foo", "/games")
    }
  }

  "linkConsistency" should {
    "at user links" in {
      copyLinkConsistency("http://example.com")
      copyLinkConsistency("https://example.com/@foo")
      copyLinkConsistency("lichess.org/@/foo")
      copyLinkConsistency("lichess.org/@/foo/games")
      copyLinkConsistency("@foo/games")
      copyLinkConsistency("@foo")
    }
  }
}
