package lila.setup

import chess.Mode
import chess.format.FEN
import lila.lobby.Color
import lila.rating.PerfType
import lila.game.PerfPicker

case class FriendConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Int,
    mode: Mode,
    color: Color,
    fen: Option[FEN] = None
) extends HumanConfig
    with Positional {

  val strictFen = false

  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, color.name, fen.map(_.value)).some

  def isPersistent = timeMode == TimeMode.Unlimited || timeMode == TimeMode.Correspondence

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(makeClock), variant, makeDaysPerTurn)
}

object FriendConfig extends BaseHumanConfig {

  def from(v: Int, tm: Int, t: Double, i: Int, d: Int, m: Option[Int], c: String, fen: Option[String]) =
    new FriendConfig(
      variant = chess.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = m.fold(Mode.default)(Mode.orDefault),
      color = Color(c) err "Invalid color " + c,
      fen = fen map FEN.apply
    )

  val default = FriendConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5d,
    increment = 8,
    days = 2,
    mode = Mode.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl._

  implicit private[setup] val friendConfigBSONHandler = new BSON[FriendConfig] {

    def reads(r: BSON.Reader): FriendConfig =
      FriendConfig(
        variant = chess.variant.Variant orDefault (r int "v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r int "i",
        days = r int "d",
        mode = Mode orDefault (r int "m"),
        color = Color.White,
        fen = r.getO[FEN]("f") filter (_.value.nonEmpty)
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
  }
}
