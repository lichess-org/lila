package lidraughts.base

import org.specs2.mutable.Specification
import play.twirl.api.Html

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
    "detect image" in {
      val url = "http://zombo.com/pic.jpg"
      addLinks(s"""img to $url here""") must_== {
        val img = s"""<img class="embed" src="$url"/>"""
        s"""img to <a rel="nofollow" href="$url" target="_blank">$img</a> here"""
      }
    }
    "detect imgur image URL" in {
      val url = "https://imgur.com/NXy19Im"
      val picUrl = "https://i.imgur.com/NXy19Im.jpg"
      val img = s"""<img class="embed" src="$picUrl"/>"""
      addLinks(s"""img to $url here""") must_==
        s"""img to <a rel="nofollow" href="$url" target="_blank">$img</a> here"""
    }
    "ignore imgur gallery URL" in {
      val url = "http://imgur.com/gallery/pMtTE"
      addLinks(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
    }

    "internal links" in {
      addLinks("lidraughts.org/@/foo/games") must_==
        """<a href="/@/foo/games">lidraughts.org/@/foo/games</a>"""
      addLinks("lidraughts.org/@/foo") must_== """<a href="/@/foo">@foo</a>"""
      addLinks("http://lidraughts.org/") must_== """<a href="/">lidraughts.org/</a>"""
      addLinks("http://lidraughts.org") must_== """<a href="/">lidraughts.org</a>"""
      addLinks("@foo") must_== """<a href="/@/foo">@foo</a>"""
    }

    "handle trailing punctuation" in {
      addLinks("lidraughts.org.") must_== """<a href="/">lidraughts.org</a>."""
      addLinks("lidraughts.org)") must_== """<a href="/">lidraughts.org</a>)"""
      addLinks("lidraughts.org/()") must_== """<a href="/()">lidraughts.org/()</a>"""

      addLinks("lidraughts.org/())") must_== """<a href="/()">lidraughts.org/()</a>)"""
      addLinks("lidraughts.org/(2)-)?") must_== """<a href="/(2)">lidraughts.org/(2)</a>-)?"""

      addLinks("lidraughts.org.-") must_== """<a href="/">lidraughts.org</a>.-"""
    }

    "handle embedded links" in {
      addLinks(".lidraughts.org") must_== """.lidraughts.org"""
      addLinks("/lidraughts.org") must_== """/lidraughts.org"""
      addLinks(".http://lidraughts.org") must_== """.<a href="/">lidraughts.org</a>"""

      addLinks("/http://lidraughts.org") must_== """/<a href="/">lidraughts.org</a>"""
    }

    "handle ambig path separator" in {
      addLinks("lidraughts.org#f") must_== """<a href="/#f">lidraughts.org/#f</a>"""
      addLinks("lidraughts.org?f") must_== """<a href="/?f">lidraughts.org/?f</a>"""
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
      expandAtUser("@foo") must_== List("lidraughts.org/@/foo")
      expandAtUser("@2foo") must_== List("lidraughts.org/@/2foo")
      expandAtUser("@foo.") must_== List("lidraughts.org/@/foo", ".")
      expandAtUser("@foo.com") must_== List("@foo.com")

      expandAtUser("@foo./") must_== List("lidraughts.org/@/foo", "./")
      expandAtUser("@foo/games") must_== List("lidraughts.org/@/foo", "/games")
    }
  }

  "linkConsistency" should {
    "at user links" in {
      copyLinkConsistency("http://example.com")
      copyLinkConsistency("https://example.com/@foo")
      copyLinkConsistency("lidraughts.org/@/foo")
      copyLinkConsistency("lidraughts.org/@/foo/games")
      copyLinkConsistency("@foo/games")
      copyLinkConsistency("@foo")
    }
  }
}
