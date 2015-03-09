package lila.evaluation

object Display {
  def assessmentString(x: Int): String =
    x match {
      case 1 => "Not cheating"
      case 2 => "Unlikely cheating"
      case 3 => "Unclear"
      case 4 => "Likely cheating"
      case 5 => "Cheating"
      case _ => "Undef"
    }

  def stockfishSig(pa: PlayerAssessment): Int =
    (pa.flags.suspiciousErrorRate, pa.flags.alwaysHasAdvantage) match {
      case (true, true)  => 5
      case (true, false) => 4
      case (false, true) => 3
      case _             => 1
    }

  def moveTimeSig(pa: PlayerAssessment): Int =
    (pa.flags.consistentMoveTimes, pa.flags.noFastMoves) match {
      case (true, true)  => 5
      case (true, false) => 4
      case (false, true) => 3
      case _             => 1
    }

  def blurSig(pa: PlayerAssessment): Int =
    (pa.flags.highBlurRate, pa.flags.moderateBlurRate) match {
      case (true, _) => 5
      case (_, true) => 4
      case _         => 1
    }

  def holdSig(pa: PlayerAssessment): Int = if (pa.flags.suspiciousHoldAlert) 5 else 1

  def emoticon(assessment: Int): String = assessment match {
    case 5 => ">:("
    case 4 => ":("
    case 3 => ":|"
    case 2 => ":)"
    case 1 => ":D"
    case _ => ":S"
  }
}