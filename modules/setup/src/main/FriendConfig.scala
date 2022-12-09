package lila.setup

import chess.format.Fen
import chess.Mode

import lila.common.Days
import lila.game.PerfPicker
import lila.lobby.Color
import lila.rating.PerfType

case class FriendConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Days,
    mode: Mode,
    color: Color,
    fen: Option[Fen.Epd] = None
) extends HumanConfig
    with Positional:

  val strictFen = false

  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, color.name, fen).some

  def isPersistent = timeMode == TimeMode.Unlimited || timeMode == TimeMode.Correspondence

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(makeClock), variant, makeDaysPerTurn)

object FriendConfig extends BaseHumanConfig:

  def from(v: Int, tm: Int, t: Double, i: Int, d: Days, m: Option[Int], c: String, fen: Option[Fen.Epd]) =
    new FriendConfig(
      variant = chess.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = m.fold(Mode.default)(Mode.orDefault),
      color = Color(c) err "Invalid color " + c,
      fen = fen
    )

  val default = FriendConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5d,
    increment = 8,
    days = Days(2),
    mode = Mode.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl.{ *, given }

  private[setup] given BSON[FriendConfig] with

    def reads(r: BSON.Reader): FriendConfig =
      FriendConfig(
        variant = chess.variant.Variant orDefault (r int "v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r int "i",
        days = r.get[Days]("d"),
        mode = Mode orDefault (r int "m"),
        color = Color.White,
        fen = r.getO[Fen.Epd]("f") filter (_.value.nonEmpty)
      )

    def writes(w: BSON.Writer, o: FriendConfig) =
      $doc(
        "v"  -> o.variant.id,
        "tm" -> o.timeMode.id,
        "t"  -> o.time,
        "i"  -> o.increment,
        "d"  -> o.days,
        "m"  -> o.mode.id,
        "f"  -> o.fen
      )
