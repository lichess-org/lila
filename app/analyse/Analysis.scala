package lila
package analyse

import chess.{ Pos, Color }
import ai.stockfish.Uci

case class Analysis(
  infos: List[Info], 
  done: Boolean, 
  fail: Option[String] = None) {

  def encode: String = infos map (_.encode) mkString Analysis.separator

  lazy val advices: List[Advice] = (infos.zipWithIndex sliding 2 map {
    case (info, turn) :: (next, _) :: Nil ⇒ Advice(info, next, turn)
    case _                                ⇒ None
  }).toList.flatten
}

sealed abstract class Severity(val delta: Int)
case object Blunder extends Severity(-300)
case object Mistake extends Severity(-100)
case object Inaccuracy extends Severity(-50)
object Severity {
  val all = List(Inaccuracy, Mistake, Blunder)
  def apply(delta: Int): Option[Severity] = all.foldLeft(none[Severity]) {
    case (_, severity) if severity.delta > delta ⇒ severity.some
    case (acc, _)                                ⇒ acc
  }
}

case class Advice(
    severity: Severity,
    info: Info,
    next: Info,
    turn: Int) {

  def color = Color(turn % 2 == 0)

  def fullMove = 1 + turn / 2
}

object Advice {

  def apply(info: Info, next: Info, turn: Int): Option[Advice] = for {
    cp ← info.score map (_.centipawns)
    nextCp ← next.score map (_.centipawns)
    color = Color(turn % 2 == 0)
    delta = nextCp - cp
    severity ← Severity(color.fold(delta, -delta))
  } yield Advice(severity, info, next, turn)
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
    move: (Pos, Pos),
    best: (Pos, Pos),
    score: Option[Score],
    mate: Option[Int]) {

  def encode: String = List(
    Uci makeMove move,
    Uci makeMove best,
    encode(score map (_.centipawns)),
    encode(mate)
  ) mkString Info.separator

  def showMove = showPoss(move)
  def showBest = showPoss(best)
  private def showPoss(poss: (Pos, Pos)) = poss._1.key + poss._2.key

  private def encode(oa: Option[Any]): String = oa.fold(_.toString, "_")
}

object Info {

  private val separator = ","

  def decode(str: String): Valid[Info] = str.split(separator).toList match {
    case moveString :: bestString :: cpString :: mateString :: Nil ⇒ Info(
      moveString, bestString, parseIntOption(cpString), parseIntOption(mateString)
    )
    case _ ⇒ !!("Invalid encoded info " + str)
  }

  def apply(
    moveString: String,
    bestString: String,
    score: Option[Int],
    mate: Option[Int]): Valid[Info] = for {
    move ← Uci parseMove moveString toValid "Invalid info move " + moveString
    best ← Uci parseMove bestString toValid "Invalid info best " + bestString
  } yield Info(
    move = move,
    best = best,
    score = score map Score.apply,
    mate = mate)
}

case class Score(centipawns: Int) {

  def pawns: Float = centipawns / 100f
  def showPawns: String = "%.2f" format pawns

  def percent: Int = math.round(box(0, 100,
    50 + (pawns / 10) * 50
  ))

  def negate = Score(-centipawns)

  private def box(min: Float, max: Float, v: Float) =
    math.min(max, math.max(min, v))
}
