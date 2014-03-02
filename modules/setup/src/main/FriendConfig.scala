package lila.setup

import chess.{ Variant, Mode, Clock, Color => ChessColor }
import lila.rating.RatingRange
import lila.game.{ Game, Player, Source }
import lila.lobby.Color

case class FriendConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    mode: Mode,
    color: Color,
    fen: Option[String] = None) extends HumanConfig with GameGenerator with Positional {

  val strictFen = false

  def >> = (variant.id, clock, time, increment, mode.id.some, color.name, fen).some

  def game = fenGame { chessGame =>
    Game.make(
      game = chessGame,
      whitePlayer = Player.white,
      blackPlayer = Player.black,
      mode = (variant == Variant.FromPosition).fold(Mode.Casual, mode),
      variant = variant,
      source = (variant == Variant.FromPosition).fold(Source.Position, Source.Friend),
      pgnImport = None)
  }

  def encode = RawFriendConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    m = mode.id,
    f = ~fen)
}

object FriendConfig extends BaseHumanConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, m: Option[Int], c: String, fen: Option[String]) =
    new FriendConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      clock = k,
      time = t,
      increment = i,
      mode = m.fold(Mode.default)(Mode.orDefault),
      color = Color(c) err "Invalid color " + c,
      fen = fen)

  val default = FriendConfig(
    variant = variantDefault,
    clock = false,
    time = 5,
    increment = 8,
    mode = Mode.default,
    color = Color.default)

  import lila.db.JsTube
  import play.api.libs.json._

  private[setup] lazy val tube = JsTube(
    reader = Reads[FriendConfig](js =>
      ~(for {
        obj ← js.asOpt[JsObject]
        raw ← RawFriendConfig.tube.read(obj).asOpt
        decoded ← raw.decode
      } yield JsSuccess(decoded): JsResult[FriendConfig])
    ),
    writer = Writes[FriendConfig](config =>
      RawFriendConfig.tube.write(config.encode) getOrElse JsUndefined("[setup] Can't write config")
    )
  )
}

private[setup] case class RawFriendConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    m: Int,
    f: String = "") {

  def decode = for {
    variant ← Variant(v)
    mode ← Mode(m)
  } yield FriendConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    mode = mode,
    color = Color.White,
    fen = f.some filter (_.nonEmpty))
}

private[setup] object RawFriendConfig {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj("f" -> none[String])

  private[setup] lazy val tube = JsTube(
    reader = (__.json update merge(defaults)) andThen Json.reads[RawFriendConfig],
    writer = Json.writes[RawFriendConfig])
}
