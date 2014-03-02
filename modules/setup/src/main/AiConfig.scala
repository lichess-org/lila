package lila.setup

import chess.{ Variant, Mode, Color => ChessColor }
import lila.game.{ Game, Player, Source }
import lila.lobby.Color

case class AiConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    level: Int,
    color: Color,
    fen: Option[String] = None) extends Config with GameGenerator with Positional {

  val strictFen = true

  def >> = (variant.id, clock, time, increment, level, color.name, fen).some

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
      pgnImport = None)
  } start

  def encode = RawAiConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    l = level,
    f = ~fen)
}

object AiConfig extends BaseConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, level: Int, c: String, fen: Option[String]) = new AiConfig(
    variant = Variant(v) err "Invalid game variant " + v,
    clock = k,
    time = t,
    increment = i,
    level = level,
    color = Color(c) err "Invalid color " + c,
    fen = fen)

  val default = AiConfig(
    variant = variantDefault,
    clock = false,
    time = 5,
    increment = 8,
    level = 1,
    color = Color.default)

  val levels = (1 to 8).toList

  val levelChoices = levels map { l => l.toString -> l.toString }

  import lila.db.JsTube
  import play.api.libs.json._

  private[setup] lazy val tube = JsTube(
    reader = Reads[AiConfig](js =>
      ~(for {
        obj ← js.asOpt[JsObject]
        raw ← RawAiConfig.tube.read(obj).asOpt
        decoded ← raw.decode
      } yield JsSuccess(decoded): JsResult[AiConfig])
    ),
    writer = Writes[AiConfig](config =>
      RawAiConfig.tube.write(config.encode) getOrElse JsUndefined("[setup] Can't write config")
    )
  )
}

private[setup] case class RawAiConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    l: Int,
    f: String = "") {

  def decode = for {
    variant ← Variant(v)
  } yield AiConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    level = l,
    color = Color.White,
    fen = f.some filter (_.nonEmpty))
}

private[setup] object RawAiConfig {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj("f" -> none[String])

  private[setup] lazy val tube = JsTube(
    reader = (__.json update merge(defaults)) andThen Json.reads[RawAiConfig],
    writer = Json.writes[RawAiConfig])
}
