package lila.setup

import chess.format.Fen
import chess.{ Mode, Clock }
import chess.variant.Variant

import lila.common.Days
import lila.lobby.Color
import lila.rating.PerfType

case class FriendConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Clock.IncrementSeconds,
    days: Days,
    mode: Mode,
    color: Color,
    fen: Option[Fen.Epd] = None
) extends HumanConfig
    with Positional:

  val strictFen = false

  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, color.name, fen).some

  def isPersistent = timeMode == TimeMode.Unlimited || timeMode == TimeMode.Correspondence

object FriendConfig extends BaseHumanConfig:

  def from(
      v: Variant.Id,
      tm: Int,
      t: Double,
      i: Clock.IncrementSeconds,
      d: Days,
      m: Option[Int],
      c: String,
      fen: Option[Fen.Epd]
  ) =
    new FriendConfig(
      variant = chess.variant.Variant.orDefault(v),
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
    increment = Clock.IncrementSeconds(8),
    days = Days(2),
    mode = Mode.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl.{ *, given }

  private[setup] given BSON[FriendConfig] with

    def reads(r: BSON.Reader): FriendConfig =
      FriendConfig(
        variant = Variant idOrDefault r.getO[Variant.Id]("v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r get "i",
        days = r.get("d"),
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
