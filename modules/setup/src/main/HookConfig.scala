package lila.setup

import chess.{ Variant, Mode, Color => ChessColor }
import lila.lobby.Color
import lila.lobby.{ Hook, Seek }
import lila.rating.RatingRange
import lila.user.User

case class HookConfig(
    variant: Variant,
    timeMode: TimeMode,
    time: Int,
    increment: Int,
    days: Int,
    mode: Mode,
    allowAnon: Boolean,
    color: Color,
    ratingRange: RatingRange) extends HumanConfig {

  // allowAnons -> membersOnly
  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, !allowAnon, ratingRange.toString.some, color.name).some

  def hook(
    uid: String,
    user: Option[User],
    sid: Option[String],
    blocking: Set[String]): Either[Hook, Option[Seek]] = timeMode match {
    case TimeMode.RealTime => Left(Hook.make(
      uid = uid,
      variant = variant,
      clock = justMakeClock,
      mode = mode,
      allowAnon = allowAnon,
      color = color.name,
      user = user,
      blocking = blocking,
      sid = sid,
      ratingRange = ratingRange))
    case _ => Right(user map { u =>
      Seek.make(
        variant = variant,
        daysPerTurn = makeDaysPerTurn,
        mode = mode,
        color = color.name,
        user = u,
        blocking = blocking,
        ratingRange = ratingRange)
    })
  }

  def noRatedUnlimited = mode.casual || hasClock || makeDaysPerTurn.isDefined
}

object HookConfig extends BaseHumanConfig {

  def <<(v: Int, tm: Int, t: Int, i: Int, d: Int, m: Option[Int], membersOnly: Boolean, e: Option[String], c: String) = {
    val realMode = m.fold(Mode.default)(Mode.orDefault)
    val useRatingRange = realMode.rated || membersOnly
    new HookConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = realMode,
      allowAnon = !membersOnly, // membersOnly
      ratingRange = e.filter(_ => useRatingRange).fold(RatingRange.default)(RatingRange.orDefault),
      color = Color(c) err "Invalid color " + c)
  }

  val default = HookConfig(
    variant = variantDefault,
    timeMode = TimeMode.RealTime,
    time = 5,
    increment = 8,
    days = 2,
    mode = Mode.default,
    allowAnon = true,
    ratingRange = RatingRange.default,
    color = Color.default)

  import reactivemongo.bson._
  import lila.db.BSON

  private[setup] implicit val hookConfigBSONHandler = new BSON[HookConfig] {

    def reads(r: BSON.Reader): HookConfig = HookConfig(
      variant = Variant orDefault (r int "v"),
      timeMode = TimeMode orDefault (r int "tm"),
      time = r int "t",
      increment = r int "i",
      days = r int "d",
      mode = Mode orDefault (r int "m"),
      allowAnon = r bool "a",
      color = Color.White,
      ratingRange = r strO "e" flatMap RatingRange.apply getOrElse RatingRange.default)

    def writes(w: BSON.Writer, o: HookConfig) = BSONDocument(
      "v" -> o.variant.id,
      "tm" -> o.timeMode.id,
      "t" -> o.time,
      "i" -> o.increment,
      "d" -> o.days,
      "m" -> o.mode.id,
      "a" -> o.allowAnon,
      "e" -> o.ratingRange.toString)
  }
}
