package lila.analyse
import chess.format.pgn.{ InitialComments, Move, Parser, Pgn, PgnStr, SanStr, Tag, Tags }
import chess.{ ByColor, Ply, Node }

import lila.core.config.{ BaseUrl, NetDomain }
import lila.core.game.PgnDump
import lila.tree.Eval
import lila.core.user.LightUserApiMinimal
import lila.core.LightUser
import lila.core.id.GamePlayerId

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
  val emptyPgn                = Pgn(Tags.empty, InitialComments.empty, None)
  def withAnnotator(pgn: Pgn) = pgn.copy(tags = pgn.tags + Tag(name = "Annotator", value = "l.org"))
  val emptyAnalysis           = Analysis(Analysis.Id(GameId("abcd")), Nil, Ply.initial, nowInstant, None)
  val emptyEval               = Eval(none, none, none)

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
  import play.api.i18n.Lang
  given Translator = new Translator:
    def to(lang: Lang): Translate = Translate(this, lang)
    def toDefault: Translate      = Translate(this, defaultLang)
    val txt = new TranslatorTxt:
      def literal(key: I18nKey, args: Seq[Any], lang: Lang): String              = key.value
      def plural(key: I18nKey, count: Count, args: Seq[Any], lang: Lang): String = key.value
    val frag = new TranslatorFrag:
      import scalatags.Text.{ Frag, RawFrag }
      def literal(key: I18nKey, args: Seq[Matchable], lang: Lang): RawFrag              = RawFrag(key.value)
      def plural(key: I18nKey, count: Count, args: Seq[Matchable], lang: Lang): RawFrag = RawFrag(key.value)

  object LightUserApi:
    def mock: LightUserApiMinimal = new:
      val sync  = LightUser.GetterSync(id => LightUser.fallback(id.into(UserName)).some)
      val async = LightUser.Getter(id => fuccess(sync(id)))

  given Lang = defaultLang

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
        Move(Ply(1), SanStr("a3")),
        Node(
          Move(Ply(2), SanStr("g6")),
          Node(
            Move(Ply(3), SanStr("g4")),
            None
          ).some
        ).some
      ).some
    )

    assertEquals(
      annotator(dumped, makeGame(playedGame), none).copy(tags = Tags.empty).render,
      PgnStr("""1. a3 { A00 Anderssen's Opening } g6 2. g4""")
    )
