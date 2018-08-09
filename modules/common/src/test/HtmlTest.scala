package lila.common

import org.specs2.mutable.Specification
import play.twirl.api.Html

class HtmlTest extends Specification {
  import String.html._

  val htmlTags = "<[^>]++>".r
  def copyLinkConsistency(text: String) = {
    // Plain text of linkified text should linkify to the same result.
    val firstHtml = addLinksRaw(text)
    val copyText = htmlTags.replaceAllIn(firstHtml, "")
    firstHtml must_== addLinksRaw(copyText)
  }

  "links" should {
    "http external" in {
      val url = "http://zombo.com"
      addLinksRaw(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
    }
    "hide https in text" in {
      val url = "zombo.com"
      addLinksRaw(s"""link to https://$url here""") must_==
        s"""link to <a rel="nofollow" href="https://$url" target="_blank">$url</a> here"""
    }
    "default to https" in {
      val url = "zombo.com"
      addLinksRaw(s"""link to $url here""") must_==
        s"""link to <a rel="nofollow" href="https://$url" target="_blank">$url</a> here"""
    }
    "skip buggy url like http://foo@bar" in {
      val url = "http://foo@bar"
      addLinksRaw(s"""link to $url here""") must not contain ("""href="http://foo"""")
    }
    "detect image" in {
      val url = "http://zombo.com/pic.jpg"
      addLinksRaw(s"""img to $url here""") must_== {
        val img = s"""<img class="embed" src="$url"/>"""
        s"""img to <a rel="nofollow" href="$url" target="_blank">$img</a> here"""
      }
    }
    "detect imgur image URL" in {
      val url = "https://imgur.com/NXy19Im"
      val picUrl = "https://i.imgur.com/NXy19Im.jpg"
      richText(s"""img to $url here""") must_== Html {
        val img = s"""<img class="embed" src="$picUrl"/>"""
        s"""img to <a rel="nofollow" href="$url" target="_blank">$img</a> here"""
      }
    }
    "ignore imgur gallery URL" in {
      val url = "http://imgur.com/gallery/pMtTE"
      richText(s"""link to $url here""") must_== Html {
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
      }
    }

    "internal links" in {
      addLinksRaw("lichess.org/@/foo/games") must_==
        """<a href="/@/foo/games">lichess.org/@/foo/games</a>"""
      addLinksRaw("lichess.org/@/foo") must_== """<a href="/@/foo">@foo</a>"""
      addLinksRaw("http://lichess.org/") must_== """<a href="/">lichess.org/</a>"""
      addLinksRaw("http://lichess.org") must_== """<a href="/">lichess.org</a>"""
      addLinksRaw("@foo") must_== """<a href="/@/foo">@foo</a>"""
    }

    "handle trailing punctuation" in {
      addLinksRaw("lichess.org.") must_== """<a href="/">lichess.org</a>."""
      addLinksRaw("lichess.org)") must_== """<a href="/">lichess.org</a>)"""
      addLinksRaw("lichess.org/()") must_== """<a href="/()">lichess.org/()</a>"""

      addLinksRaw("lichess.org/())") must_== """<a href="/()">lichess.org/()</a>)"""
      addLinksRaw("lichess.org/(2)-)?") must_== """<a href="/(2)">lichess.org/(2)</a>-)?"""

      addLinksRaw("lichess.org.-") must_== """<a href="/">lichess.org</a>.-"""
    }

    "handle embedded links" in {
      addLinksRaw(".lichess.org") must_== """.lichess.org"""
      addLinksRaw("/lichess.org") must_== """/lichess.org"""
      addLinksRaw(".http://lichess.org") must_== """.<a href="/">lichess.org</a>"""

      addLinksRaw("/http://lichess.org") must_== """/<a href="/">lichess.org</a>"""
    }

    "handle ambig path separator" in {
      addLinksRaw("lichess.org#f") must_== """<a href="/#f">lichess.org/#f</a>"""
      addLinksRaw("lichess.org?f") must_== """<a href="/?f">lichess.org/?f</a>"""
    }

    "pass through plain text (fast case)" in {
      val noUrl = "blah blah foobar"
      addLinksRaw(noUrl) must be(noUrl) // instance eq
    }
  }

  "markdown links" should {
    "add http links" in {
      val md = "[Example](http://example.com)"
      markdownLinks(md) must_== Html {
        """<a href="http://example.com">Example</a>"""
      }
    }
    "only allow safe protocols" in {
      val md = "A [link](javascript:powned) that is not safe."
      markdownLinks(md) must_== Html(md)
    }

    "addBr" in {
      markdownLinks("\n") must_== Html("<br />")
    }
  }

  "atUser" should {
    "expand valid" in {
      expandAtUserRaw("@foo") must_== List("lichess.org/@/foo")
      expandAtUserRaw("@2foo") must_== List("lichess.org/@/2foo")
      expandAtUserRaw("@foo.") must_== List("lichess.org/@/foo", ".")
      expandAtUserRaw("@foo.com") must_== List("@foo.com")

      expandAtUserRaw("@foo./") must_== List("lichess.org/@/foo", "./")
      expandAtUserRaw("@foo/games") must_== List("lichess.org/@/foo", "/games")
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
