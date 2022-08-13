package lila.common

import org.specs2.mutable.Specification

class MarkdownTest extends Specification {

  val render = new MarkdownRender()("test") _

  "autolinks" should {
    "add rel" in {
      val md = Markdown("https://example.com")
      render(
        md
      ) must_== """<p><a href="https://example.com" rel="nofollow noopener noreferrer">https://example.com</a></p>
"""
    }
  }
  "markdown links" should {
    "remove tracking tags" in {
      val md = Markdown("[Example](https://example.com?utm_campaign=spy&utm_source=evil)")
      render(
        md
      ) must_== """<p><a href="https://example.com" rel="nofollow noopener noreferrer">Example</a></p>
"""
    }
  }
  "markdown game embeds" should {
    val domain     = config.NetDomain("http://l.org")
    val gameId     = "abcdefgh"
    val pgn        = "e2 e4"
    val pgns       = Map(gameId -> pgn)
    val expander   = MarkdownRender.GameExpand(domain, pgns.get)
    val gameRender = new MarkdownRender(gameExpand = expander.some)("test") _
    "full link" in {
      val md = Markdown(s"foo [game](http://l.org/$gameId) bar")
      gameRender(
        md
      ) must_== s"""<p>foo <div data-pgn="$pgn" class="lpv--autostart">http://l.org/$gameId</div> bar</p>
"""
    }
    "auto link" in {
      val md = Markdown(s"foo http://l.org/$gameId bar")
      gameRender(
        md
      ) must_== s"""<p>foo <div data-pgn="$pgn" class="lpv--autostart">http://l.org/$gameId</div> bar</p>
"""
    }
  }
}
