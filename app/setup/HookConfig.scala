package lila
package setup

import chess.{ Variant, Mode, Color ⇒ ChessColor }
import elo.EloRange

case class HookConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    mode: Mode,
    color: Color,
    eloRange: EloRange) extends HumanConfig {

  def >> = (variant.id, clock, time, increment, mode.id, eloRange.toString, color.name).some

  def encode = RawHookConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    m = mode.id,
    e = eloRange.toString)
}

object HookConfig extends BaseHumanConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, m: Int, e: String, c: String) =
    new HookConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      clock = k,
      time = t,
      increment = i,
      mode = Mode(m) err "Invalid game mode " + m,
      eloRange = EloRange(e) err "Invalid elo range " + e,
      color = Color(c) err "Invalid color " + c)

  val default = HookConfig(
    variant = variantDefault,
    clock = true,
    time = 5,
    increment = 8,
    mode = Mode.default,
    eloRange = EloRange.default,
    color = Color.default)
}

case class RawHookConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    m: Int,
    e: String) {

  def decode = for {
    variant ← Variant(v)
    mode ← Mode(m)
    eloRange ← EloRange(e)
  } yield HookConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    mode = mode,
    eloRange = eloRange,
    color = Color.White)
}
