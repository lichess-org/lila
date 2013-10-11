package lila.analyse

import chess.format.{ UciMove, Nag }
import chess.{ Pos, Color, White, Black }
import scalaz.NonEmptyList

private[analyse] sealed trait Advice {
  def severity: Severity
  def info: Info
  def next: Info
  def text: String

  def ply = info.ply
  def turn = info.turn
  def color = info.color
  def nag = severity.nag
}

private[analyse] sealed abstract class Severity(val nag: Nag)

private[analyse] sealed abstract class CpSeverity(val delta: Int, nag: Nag) extends Severity(nag)
private[analyse] case object CpBlunder extends CpSeverity(-300, Nag.Blunder)
private[analyse] case object CpMistake extends CpSeverity(-100, Nag.Mistake)
private[analyse] case object CpInaccuracy extends CpSeverity(-50, Nag.Inaccuracy)
private[analyse] object CpSeverity {
  // note that the list order is important for the `apply` function
  private val all = List(CpBlunder, CpMistake, CpInaccuracy)
  def apply(delta: Int): Option[CpSeverity] = all.find(_.delta >= delta)
}

private[analyse] case class CpAdvice(severity: CpSeverity, info: Info, next: Info) extends Advice {
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
private[analyse] case class MateAdvice(severity: MateSeverity, info: Info, next: Info) extends Advice {
  def text = severity.toString
}

private[analyse] object Advice {

  def apply(info: Info, next: Info): Option[Advice] = {
    for {
      cp ← info.score map (_.centipawns)
      if info.hasVariation
      nextCp ← next.score map (_.centipawns)
      delta = nextCp - cp
      severity ← CpSeverity(info.color.fold(delta, -delta))
    } yield CpAdvice(severity, info, next)
  } orElse {
    MateSeverity(
      mateChance(info, info.color),
      mateChance(next, info.color)) map { MateAdvice(_, info, next) }
  }

  private def mateChance(info: Info, color: Color) =
    info.color.fold(info.mate, info.mate map (-_)) map { chance ⇒
      color.fold(chance, -chance)
    }
}

// variation is first in UCI, then converted to PGN before storage
case class Info(
    ply: Int,
    score: Option[Score],
    mate: Option[Int],
    variation: List[String]) {

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    score map (_.centipawns) getOrElse "_",
    mate getOrElse "_",
    variation mkString " "
  ) mkString Info.separator

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil)
}

object Info {

  private val separator = ","

  def decode(ply: Int, str: String): Option[Info] = str.split(separator).toList match {
    case cpString :: mateString :: rest ⇒ Info(
      ply = ply,
      score = if (cpString == "_") none else parseIntOption(cpString) map Score.apply,
      mate = if (mateString == "_") none else parseIntOption(mateString),
      variation = rest.headOption ?? (_.split(' ').toList)
    ).some
    case _ ⇒ none
  }

  def apply(score: Option[Int], mate: Option[Int], variation: List[String]): Int ⇒ Info =
    ply ⇒ Info(ply, score map Score.apply, mate, variation)
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
