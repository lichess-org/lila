package lila.tournament

case class Score(
    win: Option[Boolean],
    flag: Score.Flag) {

  val value = this match {
    case Score(Some(true), Score.Double) => 4
    case Score(Some(true), _)            => 2
    case Score(None, Score.Double)       => 2
    case Score(None, _)                  => 1
    case _                               => 0
  }
}

object Score {

  case class Sheet(scores: List[Score]) {
    val total = scores.foldLeft(0)(_ + _.value)
    def onFire = Score firstTwoAreWins scores
  }

  sealed trait Flag
  case object StreakStarter extends Flag
  case object Double extends Flag
  case object Normal extends Flag

  def sheet(user: String, tour: Tournament) = Sheet {
    val filtered = tour userPairings user filter (_.finished) reverse
    val nexts = (filtered drop 1 map Some.apply) :+ None
    filtered.zip(nexts).foldLeft(List[Score]()) {
      case (scores, (p, n)) => (p.winner match {
        case None if p.quickDraw => Score(Some(false), Normal)
        case None                => Score(None, if (firstTwoAreWins(scores)) Double else Normal)
        case Some(w) if (user == w) => Score(Some(true),
          if (firstTwoAreWins(scores)) Double
          else if (scores.headOption ?? (_.flag == StreakStarter)) StreakStarter
          else n.flatMap(_.winner) match {
            case Some(w) if (user == w) => StreakStarter
            case _                      => Normal
          })
        case _ => Score(Some(false), Normal)
      }) :: scores
    }
  }

  private def firstTwoAreWins(scores: List[Score]) =
    (scores.size >= 2) && (scores take 2 forall (~_.win))
}
