package lila.study

import chess.Ply
import chess.format.pgn.{ Comment as CommentStr, PgnStr, SanStr }
import chess.format.UciPath
import chess.variant.{ Atomic, Crazyhouse, Standard, Variant }

import lila.tree.Node.Comment
import lila.tree.{ Branch, Node, Root }

class ExplorerGameTest extends LilaTest:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Executor = scala.concurrent.ExecutionContextOpportunistic

  private def parse(pgn: PgnStr): Root = StudyPgnImport.result(pgn, Nil).toOption.get.root

  private val emptyChapter: (Node, UciPath) = Root.default(Standard) -> UciPath.root

  private def cursorAtStart(pgn: PgnStr): (Node, UciPath) = parse(pgn) -> UciPath.root

  private def cursorAt(pgn: PgnStr, ply: Int): (Node, UciPath) =
    val mainline = parse(pgn).mainline.take(ply)
    mainline.last -> UciPath.fromIds(mainline.map(_.id))

  private def cursorAtEnd(pgn: PgnStr): (Node, UciPath) = cursorAt(pgn, Int.MaxValue)

  private def merge(
      cursor: (Node, UciPath),
      game: Root,
      variant: Variant = Standard
  ): Option[(Branch, UciPath)] =
    ExplorerGameApi.merge(cursor._1, cursor._2, game, variant, GameId("explorerG"))

  private def mergeOrFail(cursor: (Node, UciPath), game: Root, variant: Variant = Standard)(using
      munit.Location
  ) =
    merge(cursor, game, variant).getOrElse(fail("expected the game to be inserted"))

  private val shuffle: PgnStr = "1. Nf3 Nf6 2. Ng1 Ng8"

  private def assertRebuilt(game: Root)(using munit.Location) =
    val (inserted, _) = mergeOrFail(cursorAtEnd(shuffle), game)
    assertEquals(inserted.mainline.map(_.move.san), game.mainline.map(_.move.san))
    assertEquals(inserted.mainline.map(_.id), game.mainline.map(_.id))
    assertEquals(inserted.mainline.map(_.fen.simple), game.mainline.map(_.fen.simple))
    assertEquals(inserted.mainline.map(_.ply), game.mainline.map(n => Ply(n.ply.value + 4)))

  test("renumber the inserted moves from the current position"):
    val chapter = parse(shuffle)
    val (inserted, path) = mergeOrFail(cursorAtEnd(shuffle), parse("1. Nc3 d5 2. e4"))
    val played = parse("1. Nf3 Nf6 2. Ng1 Ng8 3. Nc3 d5 4. e4").mainline.drop(4)
    assertEquals(path, UciPath.fromIds(chapter.mainline.map(_.id)))
    assertEquals(inserted.mainline.map(_.move.san), played.map(_.move.san))
    assertEquals(inserted.mainline.map(_.ply), played.map(_.ply))
    assertEquals(inserted.fen.value, "rnbqkbnr/pppppppp/8/8/8/2N5/PPPPPPPP/R1BQKBNR b KQkq - 5 3")
    assertEquals(inserted.mainline.map(_.fen), played.map(_.fen))
    val merged = chapter.withChildren(_.addNodeAt(inserted, path)).getOrElse(fail("expected a valid path"))
    assertEquals(merged.mainline.map(_.ply), (1 to 7).toList.map(Ply(_)))

  test("insert the game mainline only (avoid inserting the variations)"):
    val (inserted, _) = mergeOrFail(emptyChapter, parse("1. d4 d5 (1... Nf6) 2. c4"))
    assertEquals(inserted.mainline.map(_.move.san), List(SanStr("d4"), SanStr("d5"), SanStr("c4")))
    assertEquals(inserted.mainline.map(_.children.toList.size), List(1, 1, 0))

  test("insert the whole game when it starts on the starting position"):
    val (inserted, path) = mergeOrFail(emptyChapter, parse("1. e4 e5"))
    assertEquals(path, UciPath.root)
    assertEquals(inserted.mainline.map(_.move.san), List(SanStr("e4"), SanStr("e5")))
    assertEquals(inserted.mainline.map(_.ply), List(Ply(1), Ply(2)))

  test("prefer the last matching position to the start of the game"):
    val (inserted, path) = mergeOrFail(emptyChapter, parse("1. Nf3 Nf6 2. Ng1 Ng8 3. e4 e5"))
    assertEquals(path, UciPath.root)
    assertEquals(inserted.mainline.map(_.move.san), List(SanStr("e4"), SanStr("e5")))
    assertEquals(inserted.mainline.map(_.ply), List(Ply(1), Ply(2)))

  test("skip the moves the chapter already has"):
    val chapter = parse("1. d4 d5 2. c4 e6")
    val (inserted, path) = mergeOrFail(cursorAt("1. d4 d5 2. c4 e6", 2), parse("1. d4 d5 2. c4 e6 3. Nc3"))
    assertEquals(path, UciPath.fromIds(chapter.mainline.map(_.id)))
    assertEquals(inserted.move.san, SanStr("Nc3"))
    assertEquals(inserted.ply, Ply(5))

  test("follow the moves the chapter has as a variation"):
    val chapter: PgnStr = "1. d4 d5 2. c4 (2. Nf3)"
    val (inserted, path) = mergeOrFail(cursorAtStart(chapter), parse("1. d4 d5 2. Nf3 Nf6"))
    assertEquals(path, UciPath.fromIds(parse("1. d4 d5 2. Nf3").mainline.map(_.id)))
    assertEquals(inserted.move.san, SanStr("Nf6"))
    assertEquals(inserted.ply, Ply(4))

  test("renumber and skip known moves at the same time"):
    val chapter: PgnStr = "1. Nf3 Nf6 2. Ng1 Ng8 3. Nc3"
    val (inserted, path) = mergeOrFail(cursorAt(chapter, 4), parse("1. Nc3 d5 2. e4"))
    assertEquals(path, UciPath.fromIds(parse(chapter).mainline.map(_.id)))
    assertEquals(inserted.mainline.map(_.move.san), List(SanStr("d5"), SanStr("e4")))
    assertEquals(inserted.mainline.map(_.ply), List(Ply(6), Ply(7)))
    assertEquals(inserted.fen.value, "rnbqkbnr/ppp1pppp/8/3p4/8/2N5/PPPPPPPP/R1BQKBNR w KQkq - 0 4")

  test("insert nothing when the game ends on the current position"):
    val game = parse("1. d4 d5 2. Nf3 Nf6 3. Ng1 Ng8")
    assertEquals(merge(cursorAtEnd("1. d4 d5"), game), None)

  test("insert nothing when the game never reaches the current position"):
    assertEquals(merge(cursorAtEnd("1. d4 d5"), parse("1. e4 e5")), None)

  test("insert nothing when the chapter already has the whole game"):
    assertEquals(merge(cursorAtStart("1. d4 d5 2. c4"), parse("1. d4 d5 2. c4")), None)

  test("renumber from a chapter that starts on a custom position"):
    val chapter: PgnStr = """[SetUp "1"]
[FEN "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"]

1... Nf6 2. Nf3 Ng8 3. Ng1"""
    val (inserted, _) = mergeOrFail(cursorAtEnd(chapter), parse("1. e4 e5 2. Nf3"))
    assertEquals(inserted.mainline.map(_.move.san), List(SanStr("e5"), SanStr("Nf3")))
    assertEquals(inserted.mainline.map(_.ply), List(Ply(6), Ply(7)))
    assertEquals(inserted.fen.value, "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 4")

  test("rebuild promotions, castling and en passant"):
    assertRebuilt(parse("1. e4 d5 2. exd5 c6 3. dxc6 Nf6 4. cxb7 Ne4 5. bxa8=Q"))
    assertRebuilt(parse("1. e4 Nf6 2. e5 d5 3. exd6 exd6 4. Nf3 Be7 5. Bc4 O-O"))

  test("replay the game in the variant of the chapter"):
    val chapter: PgnStr = """[Variant "Crazyhouse"]

1. Nf3 Nf6 2. Ng1 Ng8"""
    val game = parse("""[Variant "Crazyhouse"]

1. e4 d5 2. exd5 Qxd5 3. P@e4 Qd8""")
    val (inserted, _) = mergeOrFail(cursorAtEnd(chapter), game, Crazyhouse)
    assertEquals(inserted.mainline.map(_.move.san), game.mainline.map(_.move.san))
    assertEquals(inserted.mainline.map(_.ply), (5 to 10).toList.map(Ply(_)))
    assertEquals(inserted.mainline.map(_.crazyData), game.mainline.map(_.crazyData))

  test("keep the source comments on the renumbered moves"):
    val game = parse("1. d4 d5 2. c4 e6")
    val comment = Comment(Comment.Id.make, CommentStr("Carlsen - Nakamura"), Comment.Author.Lichess)
    val annotated = game.setCommentAt(comment, UciPath.fromIds(game.mainline.map(_.id))).get
    val (inserted, _) = mergeOrFail(cursorAtEnd(shuffle), annotated)
    assertEquals(inserted.mainline.last.ply, Ply(8))
    assertEquals(inserted.mainline.last.comments.value.map(_.text), List(comment.text))

  test("keep the moves a game from another variant was played with".fail):
    val game = parse("1. e4 e5 2. Nf3 Nc6 3. Nxe5 Nxe5")
    val (inserted, _) = mergeOrFail(Root.default(Atomic) -> UciPath.root, game, Atomic)
    assertEquals(inserted.mainline.map(_.move.san), game.mainline.map(_.move.san))
