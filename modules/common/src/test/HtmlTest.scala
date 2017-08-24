package lila.common

import org.specs2.mutable.Specification
import play.twirl.api.Html

class HtmlTest extends Specification {

  import String.html._

  "add links" should {
    "detect link" in {
      val url = "http://zombo.com"
      addLinks(s"""link to $url here""") must_== Html {
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here"""
      }
    }
    "detect image" in {
      val url = "http://zombo.com/pic.jpg"
      addLinks(s"""img to $url here""") must_== Html {
        val img = s"""<img src="$url" width="100%" style="max-width:100%" />"""
        s"""img to <a rel="nofollow" href="$url" target="_blank">$img</a> here"""
      }
    }
    // "skip markdown images" in {
    //   val url = "http://zombo.com"
    //   addLinks(s"""img of ![some alt]($url) here""") must_== Html {
    //     s"""img of ![some alt]($url) here"""
    //   }
    // }
  }
}
