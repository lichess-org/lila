package lila.common

import scalatags.Text.all._

import org.specs2.mutable.Specification

class StringTest extends Specification {

  "slugify" should {
    "be safe in html" in {
      String.slugify("hello \" world") must not contain ("\"")
      String.slugify("<<<") must not contain ("<")
    }
  }

  "richText" should {
    "handle nl" in {
      val url = "http://imgur.com/gallery/pMtTE"
      String.html.richText(s"link to $url here\n") must_== raw {
        s"""link to <a rel="nofollow" href="$url" target="_blank">$url</a> here<br />"""
      }

      String.html.richText(s"link\n", false) must_== raw("link\n")
    }

    "escape chars" in {
      String.html.richText(s"&") must_== raw("&amp;")
    }
  }

}
