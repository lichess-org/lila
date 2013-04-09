package lila.analyse

import chess.{ Pos, Color, White, Black }
import chess.format.{ UciMove, Nag }

private[analyse] sealed trait Advice {
  def severity: Severity
  def info: Info
  def next: Info
  def text: String

  def ply = info.ply
  def turn = info.turn
  def move = info.move
  def color = info.color
  def nag = severity.nag
}

private[analyse] sealed abstract class Severity(val nag: Nag)

private[analyse] sealed abstract class CpSeverity(val delta: Int, nag: Nag) extends Severity(nag)
private[analyse] case object CpBlunder extends CpSeverity(-300, Nag.Blunder)
private[analyse] case object CpMistake extends CpSeverity(-100, Nag.Mistake)
private[analyse] case object CpInaccuracy extends CpSeverity(-50, Nag.Inaccuracy)
private[analyse] object CpSeverity {
  val all = List(CpInaccuracy, CpMistake, CpBlunder)
  def apply(delta: Int): Option[CpSeverity] = all.foldLeft(none[CpSeverity]) {
    case (_, severity) if severity.delta > delta ⇒ severity.some
    case (acc, _)                                ⇒ acc
  }
}

private[analyse] case class CpAdvice(
    severity: CpSeverity,
    info: Info,
    next: Info) extends Advice {

  def text = severity.nag.toString
}

private[analyse] sealed abstract class MateSeverity(nag: Nag, val desc: String) extends Severity(nag: Nag)
private[analyse] case class MateDelayed(before: Int, after: Int) extends MateSeverity(Nag.Inaccuracy,
  desc = "Detected checkmate in %s moves, but player moved for mate in %s".format(before, after + 1))
private[analyse] case object MateLost extends MateSeverity(Nag.Mistake,
  desc = "Lost forced checkmate sequence")
private[analyse] case object MateCreated extends MateSeverity(Nag.Blunder,
  desc = "Checkmate is now unavoidable")
private[analyse] object MateSeverity {
  def apply(current: Option[Int], next: Option[Int]): Option[MateSeverity] =
    (current, next).some collect {
      case (None, Some(n)) if n < 0                 ⇒ MateCreated
      case (Some(c), None) if c > 0                 ⇒ MateLost
      case (Some(c), Some(n)) if (c > 0) && (n < 0) ⇒ MateLost
      case (Some(c), Some(n)) if c > 0 && n >= c    ⇒ MateDelayed(c, n)
    }
}
private[analyse] case class MateAdvice(
    severity: MateSeverity,
    info: Info,
    next: Info) extends Advice {

  def text = severity.toString
}

private[analyse] object Advice {

  def apply(info: Info, next: Info): Option[Advice] = {
    for {
      cp ← info.score map (_.centipawns)
      if info.move != info.best
      nextCp ← next.score map (_.centipawns)
      delta = nextCp - cp
      severity ← CpSeverity(info.color.fold(delta, -delta))
    } yield CpAdvice(severity, info, next)
  } orElse {
    MateSeverity(
      mateChance(info, info.color),
      mateChance(next, info.color)) map { severity ⇒
        MateAdvice(severity, info, next)
      }
  }

  private def mateChance(info: Info, color: Color) =
    info.color.fold(info.mate, info.mate map (-_)) map { chance ⇒
      color.fold(chance, -chance)
    }
}

case class Info(
    ply: Int,
    move: UciMove,
    best: UciMove,
    score: Option[Score],
    mate: Option[Int]) {

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    move.piotr,
    best.piotr,
    encode(score map (_.centipawns)),
    encode(mate)
  ) mkString Info.separator

  private def encode(oa: Option[Any]): String = oa.fold("_")(_.toString)
}

object Info {

  private val separator = ","

  def decode(ply: Int, str: String): Valid[Info] = str.split(separator).toList match {
    case moveString :: bestString :: cpString :: mateString :: Nil ⇒ for {
      move ← UciMove piotr moveString toValid "Invalid info move piotr " + moveString
      best ← UciMove piotr bestString toValid "Invalid info best piotr " + bestString
    } yield Info(
      ply = ply,
      move = move,
      best = best,
      score = parseIntOption(cpString) map Score.apply,
      mate = parseIntOption(mateString))
    case _ ⇒ !!("Invalid encoded info " + str)
  }

  def apply(
    moveString: String,
    bestString: String,
    score: Option[Int],
    mate: Option[Int]): Valid[Int ⇒ Info] = for {
    move ← UciMove(moveString) toValid "Invalid info move " + moveString
    best ← UciMove(bestString) toValid "Invalid info best " + bestString
  } yield ply ⇒ Info(
    ply = ply,
    move = move,
    best = best,
    score = score map Score.apply,
    mate = mate)
}

private[analyse] case class Score(centipawns: Int) {

  def pawns: Float = centipawns / 100f
  def showPawns: String = "%.2f" format pawns

  private val percentMax = 5
  def percent: Int = math.round(box(0, 100,
    50 + (pawns / percentMax) * 50
  ))

  def negate = Score(-centipawns)

  private def box(min: Float, max: Float, v: Float) =
    math.min(max, math.max(min, v))
}
