package lila.common

import org.specs2.mutable.Specification
import play.twirl.api.Html

class HtmlTest extends Specification {

  import String.html._

  "rich text" should {
    "detect link" in {
      val url = "http://zombo.com"
      richText(s"""link to $url here""") must_== Html {
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
      }
    }
    "skip buggy url like http://foo-@bar" in {
      val url = "http://foo-@bar"
      richText(s"""link to $url here""") must_== Html {
        s"""link to http://foo-@bar here"""
      }
    }
    "detect image" in {
      val url = "http://zombo.com/pic.jpg"
      richText(s"""img to $url here""") must_== Html {
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
      val url = "https://imgur.com/gallery/pMtTE"
      richText(s"""link to $url here""") must_== Html {
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
      }
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
  }
}
