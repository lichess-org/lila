package lila.common

import org.specs2.mutable.Specification

class MarkdownTest extends Specification {

  val render = new MarkdownRender()("test") _

  "autolinks" >> {
    "add rel" >> {
      val md = Markdown("https://example.com")
      render(
        md
      ) === """<p><a href="https://example.com" rel="nofollow noopener noreferrer">https://example.com</a></p>
"""
    }
  }
  "markdown links" >> {
    "remove tracking tags" >> {
      val md = Markdown("[Example](https://example.com?utm_campaign=spy&utm_source=evil)")
      render(
        md
      ) === """<p><a href="https://example.com" rel="nofollow noopener noreferrer">Example</a></p>
"""
    }
  }
  "markdown game embeds" >> {
    val domain     = config.NetDomain("http://l.org")
    val gameId     = GameId("abcdefgh")
    val pgn        = "e2 e4"
    val pgns       = Map(gameId -> pgn)
    val expander   = MarkdownRender.GameExpand(domain, pgns.get)
    val gameRender = new MarkdownRender(gameExpand = expander.some)("test") _
    "full link" >> {
      val md = Markdown(s"foo [game](http://l.org/$gameId) bar")
      gameRender(
        md
      ) === s"""<p>foo <div data-pgn="$pgn" data-orientation="white" data-ply="" class="lpv--autostart">http://l.org/$gameId</div> bar</p>
"""
    }
    "auto link" >> {
      val md = Markdown(s"foo http://l.org/$gameId bar")
      gameRender(
        md
      ) === s"""<p>foo <div data-pgn="$pgn" data-orientation="white" data-ply="" class="lpv--autostart">http://l.org/$gameId</div> bar</p>
"""
    }
    "auto link with ply" >> {
      val md = Markdown(s"prefix http://l.org/$gameId#1 suffix")
      gameRender(
        md
      ) === s"""<p>prefix <div data-pgn="$pgn" data-orientation="white" data-ply="1" class="lpv--autostart">http://l.org/$gameId#1</div> suffix</p>
"""
    }
  }
}
