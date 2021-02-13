package lila.tournament
package arena

import org.joda.time.DateTime

case class Sheet(scores: List[Sheet.Score]) {
  val total  = scores.foldLeft(0)(_ + _.value)
  def onFire = Sheet.isOnFire(scores)
}

object Sheet {

  sealed trait Version
  case object V1 extends Version
  case object V2 extends Version // second draw gives zero point

  sealed trait Streakable
  case object Streaks   extends Streakable
  case object NoStreaks extends Streakable

  sealed abstract class Flag(val id: Int)
  case object Double        extends Flag(3)
  case object StreakStarter extends Flag(2)
  case object Normal        extends Flag(1)
  case object Null          extends Flag(0)

  sealed trait Berserk
  case object NoBerserk      extends Berserk
  case object ValidBerserk   extends Berserk
  case object InvalidBerserk extends Berserk

  sealed trait Result
  case object ResWin  extends Result
  case object ResDraw extends Result
  case object ResLoss extends Result
  case object ResDQ   extends Result

  case class Score(
      res: Result,
      flag: Flag,
      berserk: Berserk
  ) {

    def isBerserk = berserk != NoBerserk

    def isWin =
      res match {
        case ResWin  => Some(true)
        case ResLoss => Some(false)
        case _       => None
      }

    def isDraw = res == ResDraw

    val value = ((res, flag) match {
      case (ResWin, Double)  => 4
      case (ResWin, _)       => 2
      case (ResDraw, Double) => 2
      case (ResDraw, Null)   => 0
      case (ResDraw, _)      => 1
      case _                 => 0
    }) + {
      if (res == ResWin && berserk == ValidBerserk) 1 else 0
    }
  }

  val emptySheet = Sheet(Nil)

  def apply(userId: String, pairings: Pairings, version: Version, streakable: Streakable): Sheet =
    Sheet {
      val streaks = streakable == Streaks
      val nexts   = (pairings drop 1 map some) :+ None
      pairings.zip(nexts).foldLeft(List.empty[Score]) {
        case (scores, (p, n)) =>
          val berserk = if (p berserkOf userId) {
            if (p.notSoQuickFinish) ValidBerserk else InvalidBerserk
          } else NoBerserk
          (p.winner match {
            case None if p.quickDraw => Score(ResDQ, Normal, berserk)
            case None =>
              Score(
                ResDraw,
                if (streaks && isOnFire(scores)) Double
                else if (version != V1 && !p.longGame && isDrawStreak(scores)) Null
                else Normal,
                berserk
              )
            case Some(w) if userId == w =>
              Score(
                ResWin,
                if (!streaks) Normal
                else if (isOnFire(scores)) Double
                else if (scores.headOption.exists(_.flag == StreakStarter)) StreakStarter
                else
                  n match {
                    case None                                 => StreakStarter
                    case Some(s) if s.winner.contains(userId) => StreakStarter
                    case _                                    => Normal
                  },
                berserk
              )
            case _ => Score(ResLoss, Normal, berserk)
          }) :: scores
      }
    }

  private val v2date = new DateTime(2020, 4, 21, 0, 0, 0)

  def versionOf(date: DateTime) =
    if (date isBefore v2date) V1 else V2

  private def isOnFire(scores: List[Score]) =
    scores.headOption.exists(_.res == ResWin) &&
      scores.lift(1).exists(_.res == ResWin)

  @scala.annotation.tailrec
  private def isDrawStreak(scores: List[Score]): Boolean =
    scores match {
      case Nil => false
      case s :: more =>
        s.isWin match {
          case None        => true
          case Some(true)  => false
          case Some(false) => isDrawStreak(more)
        }
    }
}
