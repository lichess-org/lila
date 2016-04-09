package lila.setup

import chess.{ Mode, Color => ChessColor }
import lila.game.{ Game, Player, Source, Pov }
import lila.lobby.Color

case class AiConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Int,
    level: Int,
    color: Color,
    fen: Option[String] = None) extends Config with Positional {

  val strictFen = true

  def >> = (variant.id, timeMode.id, time, increment, days, level, color.name, fen).some

  def game = fenGame { chessGame =>
    val realVariant = chessGame.board.variant
    Game.make(
      game = chessGame,
      whitePlayer = Player.make(
        color = ChessColor.White,
        aiLevel = creatorColor.black option level),
      blackPlayer = Player.make(
        color = ChessColor.Black,
        aiLevel = creatorColor.white option level),
      mode = Mode.Casual,
      variant = realVariant,
      source = (realVariant == chess.variant.FromPosition).fold(Source.Position, Source.Ai),
      daysPerTurn = makeDaysPerTurn,
      pgnImport = None)
  } start

  def pov = Pov(game, creatorColor)
}

object AiConfig extends BaseConfig {

  def <<(v: Int, tm: Int, t: Double, i: Int, d: Int, level: Int, c: String, fen: Option[String]) = new AiConfig(
    variant = chess.variant.Variant(v) err "Invalid game variant " + v,
    timeMode = TimeMode(tm) err s"Invalid time mode $tm",
    time = t,
    increment = i,
    days = d,
    level = level,
    color = Color(c) err "Invalid color " + c,
    fen = fen)

  val default = AiConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5d,
    increment = 8,
    days = 2,
    level = 1,
    color = Color.default)

  val levels = (1 to 8).toList

  val levelChoices = levels map { l => (l.toString, l.toString, none) }

  import lila.db.BSON
  import lila.db.dsl._

  private[setup] implicit val aiConfigBSONHandler = new BSON[AiConfig] {

    override val logMalformed = false

    def reads(r: BSON.Reader): AiConfig = AiConfig(
      variant = chess.variant.Variant orDefault (r int "v"),
      timeMode = TimeMode orDefault (r int "tm"),
      time = r double "t",
      increment = r int "i",
      days = r int "d",
      level = r int "l",
      color = Color.White,
      fen = r strO "f" filter (_.nonEmpty))

    def writes(w: BSON.Writer, o: AiConfig) = $doc(
      "v" -> o.variant.id,
      "tm" -> o.timeMode.id,
      "t" -> o.time,
      "i" -> o.increment,
      "d" -> o.days,
      "l" -> o.level,
      "f" -> o.fen)
  }
}
