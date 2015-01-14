package lila.tournament
package arena

import lila.tournament.{ Score => AbstractScore }
import lila.tournament.{ ScoringSystem => AbstractScoringSystem }

object ScoringSystem extends AbstractScoringSystem {
  sealed trait Flag
  case object StreakStarter extends Flag
  case object Double extends Flag
  case object Normal extends Flag

  case class Score(
      win: Option[Boolean],
      flag: Flag,
      berserk: Int) extends AbstractScore {

    val value = ((win, flag) match {
      case (Some(true), Double) => 4
      case (Some(true), _)      => 2
      case (None, Double)       => 2
      case (None, _)            => 1
      case _                    => 0
    }) + (~win ?? berserk)
  }

  case class Sheet(scores: List[Score]) extends ScoreSheet {
    val total = scores.foldLeft(0)(_ + _.value)
    def onFire = firstTwoAreWins(scores)
  }

  override def rank(tour: Tournament, players: Players): RankedPlayers = {
    players.foldLeft(Nil: RankedPlayers) {
      case (Nil, p)                  => (1, p) :: Nil
      case (list@((r0, p0) :: _), p) => ((p0.score == p.score).fold(r0, list.size + 1), p) :: list
    }.reverse
  }

  override def scoreSheet(tour: Tournament, user: String) = Sheet {
    val filtered = tour userPairings user filter (_.finished) reverse
    val nexts = (filtered drop 1 map Some.apply) :+ None
    filtered.zip(nexts).foldLeft(List[Score]()) {
      case (scores, (p, n)) => (p.winner match {
        case None if p.quickDraw => Score(
          Some(false),
          Normal,
          p berserkOf user)
        case None => Score(
          None,
          if (firstTwoAreWins(scores)) Double else Normal,
          p berserkOf user)
        case Some(w) if (user == w) => Score(
          Some(true),
          if (firstTwoAreWins(scores)) Double
          else if (scores.headOption ?? (_.flag == StreakStarter)) StreakStarter
          else n.flatMap(_.winner) match {
            case Some(w) if (user == w) => StreakStarter
            case _                      => Normal
          },
          p berserkOf user)
        case _ => Score(Some(false), Normal, p berserkOf user)
      }) :: scores
    }
  }

  private def firstTwoAreWins(scores: List[Score]) =
    (scores.size >= 2) && (scores take 2 forall (~_.win))
}
