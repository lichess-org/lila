package lila
package analyse

import chess.{ Pos, Color, White, Black }
import chess.format.{ UciMove, Nag }

case class Analysis(
    infos: List[Info],
    done: Boolean,
    fail: Option[String] = None) {

  lazy val infoAdvices: Analysis.InfoAdvices = (infos sliding 2 collect {
    case info :: next :: Nil ⇒ info -> Advice(info, next)
  }).toList

  def advices: List[Advice] = infoAdvices.map(_._2).flatten

  lazy val advantageChart = new AdvantageChart(infoAdvices)

  def encode: String = infos map (_.encode) mkString Analysis.separator
}

object Analysis {

  type InfoAdvices = List[(Info, Option[Advice])]
  private val separator = " "

  def apply(str: String, done: Boolean): Valid[Analysis] = decode(str) map { infos ⇒
    new Analysis(infos, done)
  }

  def decode(str: String): Valid[List[Info]] =
    (str.split(separator).toList.zipWithIndex map {
      case (info, index) ⇒ Info.decode(index + 1, info)
    }).sequence

  def builder = new AnalysisBuilder(Nil)
}

sealed trait Advice {
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

sealed abstract class Severity(val nag: Nag)

sealed abstract class CpSeverity(val delta: Int, nag: Nag) extends Severity(nag)
case object CpBlunder extends CpSeverity(-300, Nag.Blunder)
case object CpMistake extends CpSeverity(-100, Nag.Mistake)
case object CpInaccuracy extends CpSeverity(-50, Nag.Inaccuracy)
object CpSeverity {
  val all = List(CpInaccuracy, CpMistake, CpBlunder)
  def apply(delta: Int): Option[CpSeverity] = all.foldLeft(none[CpSeverity]) {
    case (_, severity) if severity.delta > delta ⇒ severity.some
    case (acc, _)                                ⇒ acc
  }
}

case class CpAdvice(
    severity: CpSeverity,
    info: Info,
    next: Info) extends Advice {

  def text = severity.nag.toString
}

sealed abstract class MateSeverity(nag: Nag, val desc: String) extends Severity(nag: Nag)
case class MateDelayed(before: Int, after: Int) extends MateSeverity(Nag.Inaccuracy,
  desc = "Detected checkmate in %s moves, but player moved for mate in %s".format(before, after + 1))
case object MateLost extends MateSeverity(Nag.Mistake,
  desc = "Lost forced checkmate sequence")
case object MateCreated extends MateSeverity(Nag.Blunder,
  desc = "Checkmate is now unavoidable")
object MateSeverity {
  def apply(current: Option[Int], next: Option[Int]): Option[MateSeverity] =
    (current, next).some collect {
      case (None, Some(n)) if n < 0                 ⇒ MateCreated
      case (Some(c), None) if c > 0                 ⇒ MateLost
      case (Some(c), Some(n)) if (c > 0) && (n < 0) ⇒ MateLost
      case (Some(c), Some(n)) if c > 0 && n >= c    ⇒ MateDelayed(c, n)
    }
}
case class MateAdvice(
    severity: MateSeverity,
    info: Info,
    next: Info) extends Advice {

  def text = severity.toString
}

object Advice {

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

final class AnalysisBuilder(infos: List[Info]) {

  def size = infos.size

  def +(info: Int ⇒ Info) = new AnalysisBuilder(info(infos.size + 1) :: infos)

  def done = new Analysis(infos.reverse.zipWithIndex map {
    case (info, turn) ⇒
      (turn % 2 == 0).fold(info, info.copy(score = info.score map (_.negate)))
  }, true)
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

  private def encode(oa: Option[Any]): String = oa.fold(_.toString, "_")
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

case class Score(centipawns: Int) {

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
