package lila.common

import scalatags.Text.all.*

import lila.core.config.NetDomain

class StringTest extends munit.FunSuite:

  given NetDomain = NetDomain("lichess.org")

  test("richText handle nl") {
    val url = "http://imgur.com/gallery/pMtTE"
    assertEquals(
      String.html.richText(s"link to $url here\n"),
      raw:
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here<br>"""
    )

    assertEquals(String.html.richText(s"link\n", false), raw("link\n"))
  }

  test("richText escape chars") {
    assertEquals(String.html.richText(s"&"), raw("&amp;"))
  }

  test("richText keep trailing dash on url") {
    // We use trailing dashes (-) >> our own URL slugs. Always consider them
    // to be part of the URL.
    assertEquals(
      String.html.richText("a https://example.com/foo--. b"),
      raw:
        """a <a rel="nofollow noopener noreferrer" href="https://example.com/foo--" target="_blank">example.com/foo--</a>. b"""
    )
  }

  def extractPosts(s: String) = String.forumPostPathRegex.findAllMatchIn(s).toList.map(_.group(1))

  test("richText forum post path regex find forum post path") {
    assertEquals(
      extractPosts("[mod](https://lichess.org/@/mod) :gear: Unfeature topic  general-chess-discussion/abc"),
      List("general-chess-discussion/abc")
    )
    assertEquals(extractPosts("lichess-feedback/test-2"), List("lichess-feedback/test-2"))
    assertEquals(extractPosts("off-topic-discussion/how-come"), List("off-topic-discussion/how-come"))
    assertEquals(
      extractPosts(
        "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes xx team-4-player-chess/chess-getting-boring off-topic-discussion/how-come"
      ),
      List(
        "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes",
        "team-4-player-chess/chess-getting-boring",
        "off-topic-discussion/how-come"
      )
    )
  }

  test("richText Not find forum post path") {
    assertEquals(extractPosts("yes/no/maybe"), List())
    assertEquals(extractPosts("go/to/some/very/long/path"), List())
    assertEquals(extractPosts("Answer me yes/no?"), List())
  }

  test("noShouting") {
    assertEquals(String.noShouting("HELLO SIR"), "hello sir")
    assertEquals(String.noShouting("1. Nf3 O-O-O#"), "1. Nf3 O-O-O#")
  }
