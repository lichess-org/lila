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

    def extractPosts(s: String) = String.forumPostPathRegex.findAllMatchIn(s).toList.map(_.group(1))

    "forum post path regex" should {
      "find forum post path" in {
        extractPosts(
          "[mod](https://lichess.org/@/mod) :gear: Unfeature topic  general-chess-discussion/abc"
        ) must_== List("general-chess-discussion/abc")
        extractPosts("lichess-feedback/test-2") must_== List("lichess-feedback/test-2")
        extractPosts("off-topic-discussion/how-come") must_== List("off-topic-discussion/how-come")
        extractPosts(
          "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes xx team-4-player-chess/chess-getting-boring off-topic-discussion/how-come"
        ) must_== List(
          "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes",
          "team-4-player-chess/chess-getting-boring",
          "off-topic-discussion/how-come"
        )
      }

      "Not find forum post path" in {
        extractPosts("yes/no/maybe") must_== List()
        extractPosts("go/to/some/very/long/path") must_== List()
        extractPosts("Answer me yes/no?") must_== List()
      }
    }
  }

}
