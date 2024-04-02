package lila.analyse
import chess.format.pgn.{ InitialComments, Parser, Pgn, PgnStr, Tag, Tags }
import chess.{ ByColor, Ply }

import lila.core.config.{ BaseUrl, NetDomain }
import lila.game.PgnDump
import lila.tree.Eval

class AnnotatorTest extends munit.FunSuite:

  given Executor = scala.concurrent.ExecutionContextOpportunistic

  val annotator = Annotator(NetDomain("l.org"))
  def makeGame(g: chess.Game) =
    lila.game.Game
      .make(
        g,
        ByColor(lila.game.Player.make(_, none)),
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

  given Lang = defaultLang
  val dumper = PgnDump(BaseUrl("l.org/"), lila.user.LightUserApi.mock)
  val dumped =
    dumper(makeGame(playedGame), None, PgnDump.WithFlags(tags = false)).await(1.second, "test dump")

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
    assertEquals(
      annotator(dumped, makeGame(playedGame), none).copy(tags = Tags.empty).render,
      PgnStr("""1. a3 { A00 Anderssen's Opening } g6 2. g4""")
    )
