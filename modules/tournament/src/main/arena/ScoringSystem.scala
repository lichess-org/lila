package lila.tournament
package arena

import lila.tournament.{ Score => AbstractScore }
import lila.tournament.{ ScoringSystem => AbstractScoringSystem }

private[tournament] object ScoringSystem extends AbstractScoringSystem {

  sealed abstract class Flag(val id: Int)
  case object Double extends Flag(3)
  case object StreakStarter extends Flag(2)
  case object Normal extends Flag(1)

  sealed abstract class Berserk
  case object NoBerserk extends Berserk
  case object ValidBerserk extends Berserk
  case object InvalidBerserk extends Berserk

  case class Score(
      win: Option[Boolean],
      flag: Flag,
      berserk: Berserk
  ) extends AbstractScore {

    def isBerserk = berserk != NoBerserk

    def isWin = win contains true

    val value = ((win, flag) match {
      case (Some(true), Double) => 4
      case (Some(true), _) => 2
      case (None, Double) => 2
      case (None, _) => 1
      case _ => 0
    }) + {
      if (isWin && berserk == ValidBerserk) 1 else 0
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
        }
        else NoBerserk
        (p.winner match {
          case None if p.quickDraw => Score(Some(false), Normal, berserk)
          case None => Score(
            None,
            if (isOnFire(scores)) Double else Normal,
            berserk
          )
          case Some(w) if userId == w => Score(
            Some(true),
            if (isOnFire(scores)) Double
            else if (scores.headOption ?? (_.flag == StreakStarter)) StreakStarter
            else n.flatMap(_.winner) match {
              case Some(w) if userId == w => StreakStarter
              case _ => Normal
            },
            berserk
          )
          case _ => Score(Some(false), Normal, berserk)
        }) :: scores
    }
  }

  private def isOnFire = firstTwoAreWins _

  private def firstTwoAreWins(scores: List[Score]) =
    (scores.size >= 2) && (scores take 2 forall (~_.win))
}
