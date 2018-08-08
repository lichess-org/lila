package lidraughts.evaluation

object Display {

  def stockfishSig(pa: PlayerAssessment): Int =
    (pa.flags.suspiciousErrorRate, pa.flags.alwaysHasAdvantage) match {
      case (true, true) => 5
      case (true, false) => 4
      case (false, true) => 3
      case _ => 1
    }

  def moveTimeSig(pa: PlayerAssessment): Int =
    (pa.flags.consistentMoveTimes, pa.flags.noFastMoves) match {
      case (true, true) => 5
      case (true, false) => 4
      case (false, true) => 3
      case _ => 1
    }

  def blurSig(pa: PlayerAssessment): Int =
    (pa.flags.highBlurRate, pa.flags.moderateBlurRate) match {
      case (true, _) => 5
      case (_, true) => 4
      case _ => 1
    }

  def holdSig(pa: PlayerAssessment): Int = if (pa.flags.suspiciousHoldAlert) 5 else 1

}