package lila.setup

import shogi.Mode
import shogi.format.forsyth.Sfen
import lila.lobby.Color
import lila.rating.PerfType
import lila.game.PerfPicker

case class FriendConfig(
    variant: shogi.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    byoyomi: Int,
    periods: Int,
    days: Int,
    mode: Mode,
    color: Color,
    sfen: Option[Sfen] = None
) extends HumanConfig
    with Positional {

  val strictSfen = false

  def >> = (
    variant.id,
    timeMode.id,
    time,
    increment,
    byoyomi,
    periods,
    days,
    mode.id.some,
    color.name,
    sfen.map(_.value)
  ).some

  def isPersistent = timeMode == TimeMode.Unlimited || timeMode == TimeMode.Correspondence

  def perfType: Option[PerfType] = PerfPicker.perfType(shogi.Speed(makeClock), variant, makeDaysPerTurn)
}

object FriendConfig extends BaseHumanConfig {

  def from(
      v: Int,
      tm: Int,
      t: Double,
      i: Int,
      b: Int,
      p: Int,
      d: Int,
      m: Option[Int],
      c: String,
      sfen: Option[String]
  ) =
    new FriendConfig(
      variant = shogi.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      byoyomi = b,
      periods = p,
      days = d,
      mode = m.fold(Mode.default)(Mode.orDefault),
      color = Color(c) err "Invalid color " + c,
      sfen = sfen map Sfen.apply
    )

  val default = FriendConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5d,
    increment = 0,
    byoyomi = 10,
    periods = 1,
    days = 2,
    mode = Mode.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl._

  implicit private[setup] val friendConfigBSONHandler = new BSON[FriendConfig] {

    def reads(r: BSON.Reader): FriendConfig =
      FriendConfig(
        variant = shogi.variant.Variant orDefault (r int "v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r int "i",
        byoyomi = r intD "b",
        periods = r intD "p",
        days = r int "d",
        mode = Mode orDefault (r int "m"),
        color = Color.Sente,
        sfen = r.getO[Sfen]("f") filter (_.value.nonEmpty)
      )

    def writes(w: BSON.Writer, o: FriendConfig) =
      $doc(
        "v"  -> o.variant.id,
        "tm" -> o.timeMode.id,
        "t"  -> o.time,
        "i"  -> o.increment,
        "b"  -> o.byoyomi,
        "p"  -> o.periods,
        "d"  -> o.days,
        "m"  -> o.mode.id,
        "f"  -> o.sfen
      )
  }
}
