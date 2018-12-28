package lila.setup

import chess.Mode
import lila.lobby.Color
import lila.lobby.{ Hook, Seek }
import lila.rating.RatingRange
import lila.user.User

case class HookConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Int,
    mode: Mode,
    color: Color,
    ratingRange: RatingRange
) extends HumanConfig {

  def withinLimits(user: Option[User]): HookConfig = (for {
    pt <- perfType
    me <- user
  } yield copy(
    ratingRange = ratingRange.withinLimits(
      rating = me.perfs(pt).intRating,
      delta = 400,
      multipleOf = 50
    )
  )) | this

  private def perfType = lila.game.PerfPicker.perfType(chess.Speed(makeClock), variant, makeDaysPerTurn)

  def fixColor = copy(
    color = if (mode == Mode.Rated &&
      lila.game.Game.variantsWhereWhiteIsBetter(variant) &&
      color != Color.Random) Color.Random else color
  )

  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, ratingRange.toString.some, color.name).some

  def withTimeModeString(tc: Option[String]) = tc match {
    case Some("realTime") => copy(timeMode = TimeMode.RealTime)
    case Some("correspondence") => copy(timeMode = TimeMode.Correspondence)
    case Some("unlimited") => copy(timeMode = TimeMode.Unlimited)
    case _ => this
  }

  def hook(
    uid: lila.socket.Socket.Uid,
    user: Option[User],
    sid: Option[String],
    blocking: Set[String]
  ): Either[Hook, Option[Seek]] = timeMode match {
    case TimeMode.RealTime =>
      val clock = justMakeClock
      Left(Hook.make(
        uid = uid,
        variant = variant,
        clock = clock,
        mode = if (lila.game.Game.allowRated(variant, clock.some)) mode else Mode.Casual,
        color = color.name,
        user = user,
        blocking = blocking,
        sid = sid,
        ratingRange = ratingRange
      ))
    case _ => Right(user map { u =>
      Seek.make(
        variant = variant,
        daysPerTurn = makeDaysPerTurn,
        mode = mode,
        color = color.name,
        user = u,
        blocking = blocking,
        ratingRange = ratingRange
      )
    })
  }

  def noRatedUnlimited = mode.casual || hasClock || makeDaysPerTurn.isDefined

  def updateFrom(game: lila.game.Game) = copy(
    variant = game.variant,
    timeMode = TimeMode ofGame game,
    time = game.clock.map(_.limitInMinutes) | time,
    increment = game.clock.map(_.incrementSeconds) | increment,
    days = game.daysPerTurn | days,
    mode = game.mode
  )
}

object HookConfig extends BaseHumanConfig {

  def <<(v: Int, tm: Int, t: Double, i: Int, d: Int, m: Option[Int], e: Option[String], c: String) = {
    val realMode = m.fold(Mode.default)(Mode.orDefault)
    new HookConfig(
      variant = chess.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = realMode,
      ratingRange = e.fold(RatingRange.default)(RatingRange.orDefault),
      color = Color(c) err "Invalid color " + c
    )
  }

  val default = HookConfig(
    variant = variantDefault,
    timeMode = TimeMode.RealTime,
    time = 5d,
    increment = 8,
    days = 2,
    mode = Mode.default,
    ratingRange = RatingRange.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl._

  private[setup] implicit val hookConfigBSONHandler = new BSON[HookConfig] {

    override val logMalformed = false

    def reads(r: BSON.Reader): HookConfig = HookConfig(
      variant = chess.variant.Variant orDefault (r int "v"),
      timeMode = TimeMode orDefault (r int "tm"),
      time = r double "t",
      increment = r int "i",
      days = r int "d",
      mode = Mode orDefault (r int "m"),
      color = Color.Random,
      ratingRange = r strO "e" flatMap RatingRange.apply getOrElse RatingRange.default
    )

    def writes(w: BSON.Writer, o: HookConfig) = $doc(
      "v" -> o.variant.id,
      "tm" -> o.timeMode.id,
      "t" -> o.time,
      "i" -> o.increment,
      "d" -> o.days,
      "m" -> o.mode.id,
      "e" -> o.ratingRange.toString
    )
  }
}
