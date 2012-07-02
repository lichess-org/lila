package lila
package analyse

import chess.{ Pos, Color, White, Black }
import chess.format.UciMove

case class Analysis(
    infos: List[Info],
    done: Boolean,
    fail: Option[String] = None) {

  def encode: String = infos map (_.encode) mkString Analysis.separator

  lazy val advices: List[Advice] = (infos.zipWithIndex sliding 2 map {
    case (info, turn) :: (next, _) :: Nil ⇒ Advice(info, next, turn)
    case _                                ⇒ None
  }).toList.flatten

  def scoreRows: List[Option[List[Any]]] = infos map { info ⇒
    info.score map { score ⇒
      List(info.move.uci, score.pawns)
    }
  }

}

sealed trait Advice {
  def severity: Severity
  def info: Info
  def next: Info
  def turn: Int
  def text: String

  def color = Color(turn % 2 == 0)
  def fullMove = 1 + turn / 2

  def nag = severity.nag
}

sealed abstract class Nag(val code: Int)
case object Blunder extends Nag(4)
case object Mistake extends Nag(2)
case object Inaccuracy extends Nag(6)

sealed abstract class Severity(val nag: Nag)

sealed abstract class CpSeverity(val delta: Int, nag: Nag) extends Severity(nag)
case object CpBlunder extends CpSeverity(-300, Blunder)
case object CpMistake extends CpSeverity(-100, Mistake)
case object CpInaccuracy extends CpSeverity(-50, Inaccuracy)
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
    next: Info,
    turn: Int) extends Advice {

  def text = severity.nag.toString
}

sealed abstract class MateSeverity(nag: Nag, val desc: String) extends Severity(nag: Nag)
case object MateDelayed extends MateSeverity(Inaccuracy,
  desc = "Not the quickest path to checkmate")
case object MateLost extends MateSeverity(Mistake,
  desc = "Forced checkmate lost")
case object MateCreated extends MateSeverity(Blunder,
  desc = "Checkmate is now unavoidable")
object MateSeverity {
  def apply(current: Option[Int], next: Option[Int], turn: Int): Option[MateSeverity] =
    (current, next, Color(turn % 2 == 0)).some collect {
      case (None, Some(n), White) if n < 0              ⇒ MateCreated
      case (None, Some(n), Black) if n > 0              ⇒ MateCreated
      case (Some(c), None, White) if c > 0              ⇒ MateLost
      case (Some(c), None, Black) if c < 0              ⇒ MateLost
      case (Some(c), Some(n), White) if c > 0 && c >= n ⇒ MateDelayed
      case (Some(c), Some(n), Black) if c < 0 && c <= n ⇒ MateDelayed
    }
}
case class MateAdvice(
    severity: MateSeverity,
    info: Info,
    next: Info,
    turn: Int) extends Advice {

  def text = severity.toString
}

object Advice {

  def apply(info: Info, next: Info, turn: Int): Option[Advice] = {
    for {
      cp ← info.score map (_.centipawns)
      nextCp ← next.score map (_.centipawns)
      delta = nextCp - cp
      severity ← CpSeverity(negate(turn)(delta))
    } yield CpAdvice(severity, info, next, turn)
  } orElse {
    val mate = info.mate
    val nextMate = next.mate
    MateSeverity(mate, nextMate, turn) map { severity ⇒
      MateAdvice(severity, info, next, turn)
    }
  }

  private def negate(turn: Int)(v: Int) = (turn % 2 == 0).fold(v, -v)
}

object Analysis {

  private val separator = " "

  def apply(str: String, done: Boolean): Valid[Analysis] = decode(str) map { infos ⇒
    new Analysis(infos, done)
  }

  def decode(str: String): Valid[List[Info]] =
    (str.split(separator).toList map Info.decode).sequence

  def builder = new AnalysisBuilder(Nil)
}

final class AnalysisBuilder(infos: List[Info]) {

  def size = infos.size

  def +(info: Info) = new AnalysisBuilder(info :: infos)

  def done = new Analysis(infos.reverse.zipWithIndex map {
    case (info, turn) ⇒
      (turn % 2 == 0).fold(info, info.copy(score = info.score map (_.negate)))
  }, true)
}

case class Info(
    move: UciMove,
    best: UciMove,
    score: Option[Score],
    mate: Option[Int]) {

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

  def decode(str: String): Valid[Info] = str.split(separator).toList match {
    case moveString :: bestString :: cpString :: mateString :: Nil ⇒ for {
      move ← UciMove piotr moveString toValid "Invalid info move piotr " + moveString
      best ← UciMove piotr bestString toValid "Invalid info best piotr " + bestString
    } yield Info(
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
    mate: Option[Int]): Valid[Info] = for {
    move ← UciMove(moveString) toValid "Invalid info move " + moveString
    best ← UciMove(bestString) toValid "Invalid info best " + bestString
  } yield Info(
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
