package lila.tournament
package arena

import chess.variant.Variant

// most recent first
case class Sheet(scores: List[Sheet.Score], total: Int, variant: Variant):
  import Sheet.*

  def isOnFire: Boolean =
    scores.headOption.exists(_.res == Result.Win) &&
      scores.lift(1).exists(_.res == Result.Win)

  def addResult(userId: UserId, p: Pairing, version: Version, streakable: Streakable): Sheet =
    val berserk =
      if p.berserkOf(userId) then if p.notSoQuickFinish then Berserk.Valid else Berserk.Invalid
      else Berserk.No
    val score = p.winner match
      case None if p.quickDraw => Score(Result.DQ, Flag.Normal, berserk)
      case None =>
        Score(
          Result.Draw,
          if streakable && isOnFire then Flag.Double
          else if version != Version.V1 && !p.longGame(variant) && isDrawStreak(scores) then Flag.Null
          else Flag.Normal,
          berserk
        )
      case Some(w) if userId == w =>
        Score(
          Result.Win,
          if !streakable then Flag.Normal
          else if isOnFire then Flag.Double
          else Flag.StreakStarter,
          berserk
        )
      case _ => Score(Result.Loss, Flag.Normal, berserk)
    // update the streak flag of the previous score
    val prevScores = scores.headOption
      .filter(_.flag == Flag.StreakStarter && !p.wonBy(userId))
      .fold(scores)(_.withFlag(Flag.Normal) :: scores.tail)

    Sheet(score :: prevScores, score.value + total, variant)

  def scoresToString: String =
    val sb = new java.lang.StringBuilder(16)
    scores.foreach: score =>
      sb.append(score.value)
    sb.toString

object Sheet:
  def empty(variant: Variant) = Sheet(Nil, 0, variant)

  def buildFromScratch(
      userId: UserId,
      pairings: List[Pairing],
      version: Version,
      streakable: Streakable,
      variant: Variant
  ): Sheet =
    pairings.foldLeft(empty(variant)) { (sheet, pairing) =>
      sheet.addResult(userId, pairing, version, streakable)
    }

  opaque type Version = Int
  object Version:
    val V1: Version = 1
    val V2: Version = 2
    private val v2date = instantOf(2020, 4, 21, 0, 0)
    def of(date: Instant) = if date.isBefore(v2date) then V1 else V2

  opaque type Streakable <: Boolean = Boolean
  object Streakable:
    def apply(v: Boolean): Streakable = v

  opaque type Flag <: Int = Int
  object Flag:
    def apply(v: Int): Flag = v
    val Null = Flag(0)
    val Normal = Flag(1)
    val StreakStarter = Flag(2)
    val Double = Flag(3)

  opaque type Berserk <: Int = Int
  object Berserk:
    def apply(v: Int): Berserk = v
    val No = Berserk(0 << 2)
    val Valid = Berserk(1 << 2)
    val Invalid = Berserk(2 << 2)

  opaque type Result <: Int = Int
  object Result:
    def apply(v: Int): Result = v
    val Win = Result(0 << 4)
    val Draw = Result(1 << 4)
    val Loss = Result(2 << 4)
    val DQ = Result(3 << 4)

  opaque type Score = Int
  object Score:
    def apply(v: Int): Score = v
    def apply(res: Result, flag: Flag, berserk: Berserk): Score = Score(flag | berserk | res)
  extension (s: Score)
    // flag:    2 bits
    // berserk: 2 bits
    // result:  2 bits
    def flag: Flag = Flag(s & 0x3) // value is public
    def berserk: Berserk = Berserk(s & (0x3 << 2))
    def res: Result = Result(s & (0x3 << 4))

    def isBerserk: Boolean = berserk != Berserk.No

    def isWin: Option[Boolean] =
      res match
        case Result.Win => Some(true)
        case Result.Loss => Some(false)
        case _ => None

    def isDraw: Boolean = res == Result.Draw

    def value: Int = ((res, flag) match
      case (Result.Win, Flag.Double) => 4
      case (Result.Win, _) => 2
      case (Result.Draw, Flag.Double) => 2
      case (Result.Draw, Flag.Null) => 0
      case (Result.Draw, _) => 1
      case _ => 0
    ) + {
      if res == Result.Win && berserk == Berserk.Valid then 1 else 0
    }

    def withFlag(newFlag: Flag): Score = Score(res, newFlag, berserk)

  @scala.annotation.tailrec
  private def isDrawStreak(scores: List[Score]): Boolean =
    scores match
      case Nil => false
      case (s: Score) :: more =>
        s.isWin match
          case None => true
          case Some(true) => false
          case Some(false) => isDrawStreak(more)
