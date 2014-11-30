package lila.setup

import chess.{ Variant, Mode, Clock, Color => ChessColor }
import lila.game.{ Game, Player, Source }
import lila.lobby.Color
import lila.rating.RatingRange

case class FriendConfig(
    variant: Variant,
    timeMode: TimeMode,
    time: Int,
    increment: Int,
    days: Int,
    mode: Mode,
    color: Color,
    fen: Option[String] = None) extends HumanConfig with GameGenerator with Positional {

  val strictFen = false

  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, color.name, fen).some

  def game = fenGame { chessGame =>
    Game.make(
      game = chessGame,
      whitePlayer = Player.white,
      blackPlayer = Player.black,
      mode = (variant == Variant.FromPosition).fold(Mode.Casual, mode),
      variant = variant,
      source = (variant == Variant.FromPosition).fold(Source.Position, Source.Friend),
      daysPerTurn = makeDaysPerTurn,
      pgnImport = None)
  }

  def encode = RawFriendConfig(
    v = variant.id,
    tm = timeMode.id,
    t = time,
    i = increment,
    d = days,
    m = mode.id,
    f = ~fen)
}

object FriendConfig extends BaseHumanConfig {

  def <<(v: Int, tm: Int, t: Int, i: Int, d: Int, m: Option[Int], c: String, fen: Option[String]) =
    new FriendConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = m.fold(Mode.default)(Mode.orDefault),
      color = Color(c) err "Invalid color " + c,
      fen = fen)

  val default = FriendConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5,
    increment = 8,
    days = 2,
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
    tm: Int,
    t: Int,
    i: Int,
    d: Int,
    m: Int,
    f: String = "") {

  def decode = for {
    variant ← Variant(v)
    mode ← Mode(m)
    timeMode <- TimeMode(tm)
  } yield FriendConfig(
    variant = variant,
    timeMode = timeMode,
    time = t,
    increment = i,
    days = d,
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
