package lila.common

import chess.format.pgn.PgnStr
import lila.common.config.AssetDomain

class MarkdownTest extends munit.FunSuite {

  val render: Markdown => Html = new MarkdownRender(assetDomain = AssetDomain("lichess1.org").some)("test") _

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
  val gameId     = GameId("gameId12")
  val studyId    = StudyId("StudyId1")
  val chapterId  = StudyChapterId("ChaptId1")
  val gamePgn    = PgnStr("e2 e4")
  val gameUrl    = s"http://l.org/$gameId"
  val chapterPgn = PgnStr("Nf3 Nf6 d4")
  val chapterUrl = s"http://l.org/study/$studyId/$chapterId"
  val pgns       = Map(gameId.value -> gamePgn, chapterId.value -> chapterPgn)
  val expander   = MarkdownRender.PgnSourceExpand(domain, pgns.get)
  val mdRender   = MarkdownRender(pgnExpand = expander.some)("test") _

  test("markdown game embeds full link") {
    val md = Markdown(s"foo [game]($gameUrl) bar")
    assertEquals(
      mdRender(md),
      Html:
        s"""<p>foo <div data-pgn="$gamePgn" class="lpv--autostart is2d">$gameUrl</div> bar</p>
"""
    )
  }
  test("markdown game embeds auto link") {
    val md = Markdown(s"foo $gameUrl bar")
    assertEquals(
      mdRender(md),
      Html:
        s"""<p>foo <div data-pgn="$gamePgn" class="lpv--autostart is2d">$gameUrl</div> bar</p>
"""
    )
  }
  test("markdown game embeds auto link with ply") {
    val md = Markdown(s"prefix $gameUrl#1 suffix")
    assertEquals(
      mdRender(md),
      Html:
        s"""<p>prefix <div data-pgn="$gamePgn" class="lpv--autostart is2d" data-ply="1">$gameUrl#1</div> suffix</p>
"""
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
  test("markdown chapter embed auto link") {
    val md = Markdown(s"foo $chapterUrl bar")
    assertEquals(
      mdRender(md),
      Html:
        s"""<p>foo <div data-pgn="$chapterPgn" class="lpv--autostart is2d">$chapterUrl</div> bar</p>
"""
    )
  }
}
