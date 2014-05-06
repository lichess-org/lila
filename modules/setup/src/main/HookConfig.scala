package lila.setup

import chess.{ Variant, Mode, Color => ChessColor }
import lila.rating.RatingRange
import lila.lobby.Color
import lila.lobby.Hook
import lila.user.User

case class HookConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    mode: Mode,
    allowAnon: Boolean,
    color: Color,
    ratingRange: RatingRange) extends HumanConfig {

  // allowAnons -> membersOnly
  def >> = (variant.id, clock, time, increment, mode.id.some, !allowAnon, ratingRange.toString.some, color.name).some

  def hook(uid: String, user: Option[User], sid: Option[String]) = Hook.make(
    uid = uid,
    variant = variant,
    clock = makeClock,
    mode = user.fold(mode)(_.engine.fold(Mode.Casual, mode)),
    allowAnon = allowAnon,
    color = color.name,
    user = user,
    sid = sid,
    ratingRange = ratingRange)

  def encode = RawHookConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    m = mode.id,
    a = allowAnon,
    e = ratingRange.toString)

  def noRatedUnlimited = mode.casual || clock
}

object HookConfig extends BaseHumanConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, m: Option[Int], membersOnly: Boolean, e: Option[String], c: String) = {
    val realMode = m.fold(Mode.default)(Mode.orDefault)
    val useRatingRange = realMode.rated || membersOnly
    new HookConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      clock = k,
      time = t,
      increment = i,
      mode = realMode,
      allowAnon = !membersOnly, // membersOnly
      ratingRange = e.filter(_ => useRatingRange).fold(RatingRange.default)(RatingRange.orDefault),
      color = Color(c) err "Invalid color " + c)
  }

  val default = HookConfig(
    variant = variantDefault,
    clock = true,
    time = 5,
    increment = 8,
    mode = Mode.default,
    allowAnon = true,
    ratingRange = RatingRange.default,
    color = Color.default)

  import lila.db.JsTube
  import play.api.libs.json._

  private[setup] lazy val tube = JsTube(
    reader = Reads[HookConfig](js =>
      ~(for {
        obj ← js.asOpt[JsObject]
        raw ← RawHookConfig.tube.read(obj).asOpt
        decoded ← raw.decode
      } yield JsSuccess(decoded): JsResult[HookConfig])
    ),
    writer = Writes[HookConfig](config =>
      RawHookConfig.tube.write(config.encode) getOrElse JsUndefined("[setup] Can't write config")
    )
  )
}

private[setup] case class RawHookConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    m: Int,
    a: Boolean,
    e: String) {

  def decode = for {
    variant ← Variant(v)
    mode ← Mode(m)
    ratingRange ← RatingRange(e)
  } yield HookConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    mode = mode,
    allowAnon = a,
    ratingRange = ratingRange,
    color = Color.White)
}

private[setup] object RawHookConfig {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private[setup] lazy val tube = JsTube(
    __.json update merge(defaults) andThen Json.reads[RawHookConfig],
    Json.writes[RawHookConfig])

  private def defaults = Json.obj("a" -> true)
}
