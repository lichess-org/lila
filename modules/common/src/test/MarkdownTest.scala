package lila.common

import chess.format.pgn.PgnStr

class MarkdownTest extends munit.FunSuite {

  val render: Markdown => Html = new MarkdownRender()("test") _

  test("autolinks add rel") {
    val md = Markdown("https://example.com")
    assertEquals(
      render(md),
      Html(
        """<p><a href="https://example.com" rel="nofollow noopener noreferrer">https://example.com</a></p>
"""
      )
    )
  }
  test("markdown links remove tracking tags") {
    val md = Markdown("[Example](https://example.com?utm_campaign=spy&utm_source=evil)")
    assertEquals(
      render(md),
      Html("""<p><a href="https://example.com" rel="nofollow noopener noreferrer">Example</a></p>
""")
    )
  }
  val domain     = config.NetDomain("http://l.org")
  val gameId     = GameId("abcdefgh")
  val pgn        = PgnStr("e2 e4")
  val pgns       = Map(gameId -> pgn)
  val expander   = MarkdownRender.GameExpand(domain, pgns.get)
  val gameRender = new MarkdownRender(gameExpand = expander.some)("test") _
  test("markdown game embeds full link") {
    val md = Markdown(s"foo [game](http://l.org/$gameId) bar")
    assertEquals(
      gameRender(md),
      Html(
        s"""<p>foo <div data-pgn="$pgn" data-orientation="white" data-ply="" class="lpv--autostart">http://l.org/$gameId</div> bar</p>
"""
      )
    )
  }
  test("markdown game embeds auto link") {
    val md = Markdown(s"foo http://l.org/$gameId bar")
    assertEquals(
      gameRender(md),
      Html(
        s"""<p>foo <div data-pgn="$pgn" data-orientation="white" data-ply="" class="lpv--autostart">http://l.org/$gameId</div> bar</p>
"""
      )
    )
  }
  test("markdown game embeds auto link with ply") {
    val md = Markdown(s"prefix http://l.org/$gameId#1 suffix")
    assertEquals(
      gameRender(md),
      Html(
        s"""<p>prefix <div data-pgn="$pgn" data-orientation="white" data-ply="1" class="lpv--autostart">http://l.org/$gameId#1</div> suffix</p>
"""
      )
    )
  }
  test("markdown image whitelist pass") {
    assertEquals(
      render(Markdown("![image](https://lichess1.org/image.png)")),
      Html("""<p><img src="https://lichess1.org/image.png" alt="image" /></p>
""")
    )
  }
  test("markdown image whitelist block") {
    assertEquals(
      render(Markdown("![image](https://evil.com/image.png)")),
      Html("""<p><a href="https://evil.com/image.png" rel="nofollow noopener noreferrer">image</a></p>
""")
    )
  }
}
