package lila.tournament
package arena

import lila.tournament.{ Score => AbstractScore }
import lila.tournament.{ ScoringSystem => AbstractScoringSystem }

private[tournament] object ScoringSystem extends AbstractScoringSystem {

  sealed abstract class Flag(val id: Int)
  case object Double extends Flag(3)
  case object StreakStarter extends Flag(2)
  case object Normal extends Flag(1)

  sealed trait Berserk
  case object NoBerserk extends Berserk
  case object ValidBerserk extends Berserk
  case object InvalidBerserk extends Berserk

  sealed trait Result
  case object ResWin extends Result
  case object ResDraw extends Result
  case object ResLoss extends Result
  case object ResDQ extends Result

  case class Score(
      res: Result,
      flag: Flag,
      berserk: Berserk
  ) extends AbstractScore {

    def isBerserk = berserk != NoBerserk

    val value = ((res, flag) match {
      case (ResWin, Double) => 4
      case (ResWin, _) => 2
      case (ResDraw, Double) => 2
      case (ResDraw, _) => 1
      case _ => 0
    }) + {
      if (res == ResWin && berserk == ValidBerserk) 1 else 0
    }
  }

  case class Sheet(scores: List[Score]) extends ScoreSheet {
    val total = scores.foldLeft(0)(_ + _.value)
    def onFire = isOnFire(scores)
  }

  val emptySheet = Sheet(Nil)

  def sheet(userId: String, pairings: Pairings): Sheet = Sheet {
    val nexts = (pairings drop 1 map some) :+ None
    pairings.zip(nexts).foldLeft(List.empty[Score]) {
      case (scores, (p, n)) =>
        val berserk = if (p berserkOf userId) {
          if (p.notSoQuickFinish) ValidBerserk else InvalidBerserk
        } else NoBerserk
        (p.winner match {
          case None if p.quickDraw => Score(ResDQ, Normal, berserk)
          case None => Score(
            ResDraw,
            if (isOnFire(scores)) Double else Normal,
            berserk
          )
          case Some(w) if userId == w => Score(
            ResWin,
            if (isOnFire(scores)) Double
            else if (scores.headOption ?? (_.flag == StreakStarter)) StreakStarter
            else n match {
              case None => StreakStarter
              case Some(s) if s.winner.contains(userId) => StreakStarter
              case _ => Normal
            },
            berserk
          )
          case _ => Score(ResLoss, Normal, berserk)
        }) :: scores
    }
  }

  private def isOnFire = firstTwoAreWins _

  private def firstTwoAreWins(scores: List[Score]) = scores match {
    case Score(ResWin, _, _) :: Score(ResWin, _, _) :: _ => true
    case _ => false
  }
}
