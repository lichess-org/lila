package lila.analyse
import chess.format.pgn.{ InitialComments, Move, Parser, Pgn, PgnStr, SanStr, Tag, Tags }
import chess.{ ByColor, Node, Ply }

import lila.core.LightUser
import lila.core.config.NetDomain
import lila.core.id.GamePlayerId
import lila.core.user.LightUserApiMinimal
import lila.tree.Eval

class AnnotatorTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContextOpportunistic

  val annotator = Annotator(NetDomain("l.org"))
  def makeGame(g: chess.Game) =
    lila.core.game
      .newGame(
        g,
        ByColor(lila.core.game.Player(GamePlayerId("abcd"), _, aiLevel = none)),
        mode = chess.Mode.Casual,
        source = lila.core.game.Source.Api,
        pgnImport = none
      )
      .sloppy
  val emptyPgn                = Pgn(Tags.empty, InitialComments.empty, None, Ply.initial)
  def withAnnotator(pgn: Pgn) = pgn.copy(tags = pgn.tags + Tag(name = "Annotator", value = "l.org"))
  val emptyAnalysis = Analysis(Analysis.Id(GameId("abcd")), Nil, Ply.initial, nowInstant, None, None)
  val emptyEval     = Eval(none, none, none)

  val pgnStr = PgnStr("""1. a3 g6?! 2. g4""")
  val playedGame: chess.Game =
    chess.format.pgn.Reader
      .fullWithSans(
        Parser.full(pgnStr).toOption.get,
        identity
      )
      .valid
      .toOption
      .get
      .state

  import lila.core.i18n.*
  given Translator         = TranslatorStub
  given play.api.i18n.Lang = defaultLang

  object LightUserApi:
    def mock: LightUserApiMinimal = new:
      val sync  = LightUser.GetterSync(id => LightUser.fallback(id.into(UserName)).some)
      val async = LightUser.Getter(id => fuccess(sync(id)))

  test("empty game"):
    assertEquals(
      annotator(emptyPgn, makeGame(chess.Game(chess.variant.Standard)), none),
      withAnnotator(emptyPgn)
    )

  test("empty analysis"):
    assertEquals(
      annotator(emptyPgn, makeGame(chess.Game(chess.variant.Standard)), emptyAnalysis.some),
      withAnnotator(emptyPgn)
    )

  test("opening comment"):
    val dumped = Pgn(
      Tags.empty,
      InitialComments.empty,
      Node(
        Move(SanStr("a3")),
        Node(
          Move(SanStr("g6")),
          Node(
            Move(SanStr("g4")),
            None
          ).some
        ).some
      ).some,
      Ply.firstMove
    )

    assertEquals(
      annotator(dumped, makeGame(playedGame), none).copy(tags = Tags.empty).render,
      PgnStr("""1. a3 { A00 Anderssen's Opening } g6 2. g4""")
    )
