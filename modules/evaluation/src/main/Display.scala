package lila.evaluation

object Display:

  def stockfishSig(pa: PlayerAssessment): Int =
    (pa.flags.suspiciousErrorRate, pa.flags.alwaysHasAdvantage) match
      case (true, true)  => 5
      case (true, false) => 4
      case (false, true) => 3
      case _             => 1

  def moveTimeSig(pa: PlayerAssessment): Int =
    (pa.flags.highlyConsistentMoveTimes, pa.flags.moderatelyConsistentMoveTimes, pa.flags.noFastMoves) match
      case (true, _, _)         => 5
      case (false, true, true)  => 4
      case (false, true, false) => 3
      case (false, false, true) => 2
      case _                    => 1

  def blurSig(pa: PlayerAssessment): Int =
    (pa.flags.highBlurRate, pa.flags.moderateBlurRate) match
      case (true, _) => 5
      case (_, true) => 4
      case _         => 1

  def holdSig(pa: PlayerAssessment): Int = if pa.flags.suspiciousHoldAlert then 5 else 1
