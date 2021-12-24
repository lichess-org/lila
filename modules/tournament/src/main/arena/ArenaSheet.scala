package lila.tournament
package arena

import org.joda.time.DateTime
import lila.user.User

// most recent first
case class Sheet(scores: List[Sheet.Score]) {
  val total  = scores.foldLeft(0)(_ + _.value)
  def onFire = Sheet.isOnFire(scores)
}

object Sheet {

  // Using Int/Boolean instead of sealed traits
  // because loads of them are in heap cache

  type Version = Int
  val V1 = 1
  val V2 = 2 // second draw gives zero point

  type Streakable = Boolean
  val NoStreaks = false
  val Streaks   = true

  type Flag = Int
  val Null          = 0
  val Normal        = 1
  val StreakStarter = 2
  val Double        = 3

  type Berserk = Int
  val NoBerserk      = 0
  val ValidBerserk   = 1
  val InvalidBerserk = 2

  type Result = Int
  val ResWin  = 0
  val ResDraw = 1
  val ResLoss = 2
  val ResDQ   = 3

  final class Score(val res: Result, val flag: Flag, val berserk: Berserk) {

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

    def withFlag(newFlag: Flag) = new Score(res, newFlag, berserk)
  }

  val emptySheet = Sheet(Nil)

  def buildFromScratch(
      userId: User.ID,
      pairings: List[Pairing],
      version: Version,
      streakable: Streakable
  ): Sheet =
    Sheet {
      val nexts = (pairings drop 1 map some) :+ None
      pairings.zip(nexts).foldLeft(List.empty[Score]) { case (scores, (p, n)) =>
        val berserk = if (p berserkOf userId) {
          if (p.notSoQuickFinish) ValidBerserk else InvalidBerserk
        } else NoBerserk
        (p.winner match {
          case None if p.quickDraw => new Score(ResDQ, Normal, berserk)
          case None =>
            new Score(
              ResDraw,
              if (streakable && isOnFire(scores)) Double
              else if (version != V1 && !p.longGame && isDrawStreak(scores)) Null
              else Normal,
              berserk
            )
          case Some(w) if userId == w =>
            new Score(
              ResWin,
              if (!streakable) Normal
              else if (isOnFire(scores)) Double
              else if (scores.headOption.exists(_.flag == StreakStarter)) StreakStarter
              else
                n match {
                  case None                       => StreakStarter
                  case Some(s) if s.wonBy(userId) => StreakStarter
                  case _                          => Normal
                },
              berserk
            )
          case _ => new Score(ResLoss, Normal, berserk)
        }) :: scores
      }
    }

  def addResult(sheet: Sheet, userId: User.ID, p: Pairing, streakable: Streakable): Sheet =
    Sheet {
      val scores = sheet.scores
      val berserk = if (p berserkOf userId) {
        if (p.notSoQuickFinish) ValidBerserk else InvalidBerserk
      } else NoBerserk
      val score = p.winner match {
        case None if p.quickDraw => new Score(ResDQ, Normal, berserk)
        case None =>
          new Score(
            ResDraw,
            if (streakable && isOnFire(scores)) Double
            else if (!p.longGame && isDrawStreak(scores)) Null
            else Normal,
            berserk
          )
        case Some(w) if userId == w =>
          new Score(
            ResWin,
            if (!streakable) Normal
            else if (isOnFire(scores)) Double
            else StreakStarter,
            berserk
          )
        case _ => new Score(ResLoss, Normal, berserk)
      }
      // update the streak flag of the previous score
      val prevScores = scores.headOption
        .filter(_.flag == StreakStarter && !p.wonBy(userId))
        .fold(scores)(_.withFlag(Normal) :: scores.tail)

      score :: prevScores
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
