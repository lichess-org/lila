package lila.mod

object AutoAnalysis {

  sealed trait Reason

  object Reason {

    case object Upset                extends Reason
    case object HoldAlert            extends Reason
    case object WhiteMoveTime        extends Reason
    case object BlackMoveTime        extends Reason
    case object Blurs                extends Reason
    case object WinnerRatingProgress extends Reason
    case object NewPlayerWin         extends Reason
  }
}
