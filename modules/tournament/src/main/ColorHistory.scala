package lila.tournament
//positive strike -> user played straight strike games by white pieces
//negative strike -> black pieces
class ColorHistory private (val strike: Int, val balance: Int) extends Ordered[ColorHistory] {
  import ColorHistory.{ hi, lo }
  def toInt             = ((strike - lo) << 16) | (balance - lo)
  override def hashCode = toInt
  override def equals(other: Any) =
    other match {
      case that: ColorHistory => strike == that.strike && balance == that.balance
      case _                  => false
    }
  override def compare(that: ColorHistory): Int = {
    if (strike < that.strike) -1
    else if (strike > that.strike) 1
    else if (balance < that.balance) -1
    else if (balance > that.balance) 1
    else 0
  }
  def firstGetsWhite(that: ColorHistory)(firstGetsWhite: () => Boolean) = {
    val c = compare(that)
    c < 0 || (c == 0 && firstGetsWhite())
  }
  //value > 0 -> user plays by white pieces
  //value < 0 -> user plays by black pieces
  //returns packed value after updating color history
  def incColor(value: Int): ColorHistory = {
    if (value > 0) {
      new ColorHistory((strike + 1).max(1).min(hi), (balance + 1).min(hi))
    } else {
      new ColorHistory((strike - 1).min(-1).max(lo), (balance - 1).max(lo))
    }
  }
  //couldn't play if both players played maxStrike blacks games before
  //or both player maxStrike games before
  def couldPlay(that: ColorHistory, maxStrike: Int): Boolean = {
    (strike > -maxStrike || that.strike > -maxStrike) &&
    (strike < maxStrike || that.strike < maxStrike)
  }
  //add some penalty for pairs when both players have played last game with same color
  //heuristics: after such pairing one streak will be always incremented
  def sameColors(that: ColorHistory): Boolean = strike.sign * that.strike.sign > 0
}

object ColorHistory {
  private val lo   = -0x8000
  private val hi   = 0x7fff
  private val mask = 0xffff
  val empty        = new ColorHistory(0, 0)
  def apply(o: Option[Int]): ColorHistory = {
    o match {
      case Some(v) => new ColorHistory((v >>> 16) + lo, (v & mask) + lo)
      case None    => empty
    }
  }
  def minValue: ColorHistory = apply(Some(0))
  def maxValue: ColorHistory = apply(Some(-1))
}
