package lila.common

import scalatags.Text.all._

import org.specs2.mutable.Specification

class StringTest extends Specification {

  "slugify" should {
    "be safe in html" in {
      String.slugify("hello \" world") must not contain "\""
      String.slugify("<<<") must not contain "<"
    }
  }

  "richText" should {
    "handle nl" in {
      val url = "http://imgur.com/gallery/pMtTE"
      String.html.richText(s"link to $url here\n") must_== raw {
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here<br>"""
      }

      String.html.richText(s"link\n", false) must_== raw("link\n")
    }

    "escape chars" in {
      String.html.richText(s"&") must_== raw("&amp;")
    }

    "keep trailing dash on url" in {
      // We use trailing dashes (-) in our own URL slugs. Always consider them
      // to be part of the URL.
      String.html.richText("a https://example.com/foo--. b") must_== raw {
        """a <a rel="nofollow noopener noreferrer" href="https://example.com/foo--" target="_blank">example.com/foo--</a>. b"""
      }
    }

    "prize regex" should {
      "not find btc in url" in {
        String.looksLikePrize(s"HqVrbTcy") must beFalse
        String.looksLikePrize(s"10btc") must beTrue
        String.looksLikePrize(s"ten btc") must beTrue
      }
    }
  }

}
