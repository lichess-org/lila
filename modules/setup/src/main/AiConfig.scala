package lila.setup

import chess.{ Variant, Mode, Color => ChessColor }
import lila.game.{ Game, Player, Source }
import lila.lobby.Color

case class AiConfig(
    variant: Variant,
    timeMode: TimeMode,
    time: Int,
    increment: Int,
    days: Int,
    level: Int,
    color: Color,
    fen: Option[String] = None) extends Config with GameGenerator with Positional {

  val strictFen = true

  def >> = (variant.id, timeMode.id, time, increment, days, level, color.name, fen).some

  def game = fenGame { chessGame =>
    Game.make(
      game = chessGame,
      whitePlayer = Player.make(
        color = ChessColor.White,
        aiLevel = creatorColor.black option level),
      blackPlayer = Player.make(
        color = ChessColor.Black,
        aiLevel = creatorColor.white option level),
      mode = Mode.Casual,
      variant = variant,
      source = (variant == Variant.FromPosition).fold(Source.Position, Source.Ai),
      daysPerTurn = makeDaysPerTurn,
      pgnImport = None)
  } start
}

object AiConfig extends BaseConfig {

  def <<(v: Int, tm: Int, t: Int, i: Int, d: Int, level: Int, c: String, fen: Option[String]) = new AiConfig(
    variant = Variant(v) err "Invalid game variant " + v,
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
    time = 5,
    increment = 8,
    days = 2,
    level = 1,
    color = Color.default)

  val levels = (1 to 8).toList

  val levelChoices = levels map { l => (l.toString, l.toString, none) }

  import reactivemongo.bson._
  import lila.db.BSON

  private[setup] implicit val aiConfigBSONHandler = new BSON[AiConfig] {

    def reads(r: BSON.Reader): AiConfig = AiConfig(
      variant = Variant orDefault (r int "v"),
      timeMode = TimeMode orDefault (r int "tm"),
      time = r int "t",
      increment = r int "i",
      days = r int "d",
      level = r int "l",
      color = Color.White,
      fen = r strO "f" filter (_.nonEmpty))

    def writes(w: BSON.Writer, o: AiConfig) = BSONDocument(
      "v" -> o.variant.id,
      "tm" -> o.timeMode.id,
      "t" -> o.time,
      "i" -> o.increment,
      "d" -> o.days,
      "l" -> o.level,
      "f" -> o.fen)
  }
}
