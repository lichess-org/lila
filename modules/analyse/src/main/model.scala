package lila.analyse

import chess.format.{ UciMove, Nag }
import chess.{ Pos, Color, White, Black }
import scalaz.NonEmptyList

private[analyse] sealed trait Advice {
  def severity: Severity
  def info: Info
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

private[analyse] case class CpAdvice(severity: CpSeverity, info: Info) extends Advice {
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
  def apply(prev: Option[Int], next: Option[Int]): Option[MateSeverity] =
    (prev, next).some collect {
      case (None, Some(n)) if n < 0                 ⇒ MateCreated
      case (Some(p), None) if p > 0                 ⇒ MateLost
      case (Some(p), Some(n)) if (p > 0) && (n < 0) ⇒ MateLost
      case (Some(p), Some(n)) if p > 0 && n >= p    ⇒ MateDelayed(p, n)
    }
}
private[analyse] case class MateAdvice(severity: MateSeverity, info: Info) extends Advice {
  def text = severity.toString
}

private[analyse] object Advice {

  def apply(prev: Info, info: Info): Option[Advice] = {
    for {
      cp ← prev.score map (_.centipawns)
      infoCp ← info.score map (_.centipawns)
      delta = infoCp - cp
      severity ← CpSeverity(info.color.fold(delta, -delta))
    } yield CpAdvice(severity, info)
  } orElse {
    MateSeverity(prev.mate, info.mate) map { MateAdvice(_, info) }
  }
}

case class Evaluation(
    lastMove: Option[String],
    score: Option[Score],
    mate: Option[Int],
    line: List[String]) {

  override def toString = s"Evaluation ${score.fold("?")(_.showPawns)} ${mate | 0} ${line.mkString(" ")}"
}
object Evaluation {

  lazy val start = Evaluation(none, Score(20).some, none, Nil)

  def toInfos(evals: List[Evaluation], moves: List[String]): List[Info] =
    (evals sliding 2).toList.zip(moves).zipWithIndex map {
      case ((List(before, after), move), index) ⇒ {
        val info = Info(
          ply = index + 1,
          score = after.score,
          mate = after.mate,
          variation = before.line match {
            case first :: rest if first != move ⇒ first :: rest
            case _                              ⇒ Nil
          }) |> { info ⇒
            if (info.ply % 2 == 1) info.reverse else info
          }
        println(s"""$move $info""")
        info
      }
    }
}

// variation is first in UCI, then converted to PGN before storage
case class Info(
    ply: Int,
    score: Option[Score] = None,
    mate: Option[Int] = None,
    variation: List[String] = Nil) {

  def turn = 1 + (ply - 1) / 2

  def color = Color(ply % 2 == 1)

  def encode: String = List(
    score ?? (_.centipawns.toString),
    mate ?? (_.toString),
    variation mkString " "
  ) mkString Info.separator

  def hasVariation = variation.nonEmpty
  def dropVariation = copy(variation = Nil)

  def reverse = copy(score = score map (-_), mate = mate map (-_))

  override def toString = s"Info [$ply] ${score.fold("?")(_.showPawns)} ${mate | 0} ${variation.mkString(" ")}"
}

object Info {

  private val separator = ","
  private val listSeparator = ";"

  lazy val start = Info(0, Evaluation.start.score, none, Nil)

  def decode(ply: Int, str: String): Option[Info] = str.split(separator).toList match {
    case cp :: Nil             ⇒ Info(ply, Score(cp)).some
    case cp :: ma :: Nil       ⇒ Info(ply, Score(cp), parseIntOption(ma)).some
    case cp :: ma :: va :: Nil ⇒ Info(ply, Score(cp), parseIntOption(ma), va.split(' ').toList).some
    case _                     ⇒ none
  }

  def decodeList(str: String): Option[List[Info]] = {
    str.split(listSeparator).toList.zipWithIndex map {
      case (infoStr, index) ⇒ decode(index + 1, infoStr)
    }
  }.sequence

  def encodeList(infos: List[Info]): String = infos map (_.encode) mkString listSeparator

  def apply(score: Option[Int], mate: Option[Int], variation: List[String]): Int ⇒ Info =
    ply ⇒ Info(ply, score map Score.apply, mate, variation)
}

case class Score(centipawns: Int) {
  def pawns: Float = centipawns / 100f
  def showPawns: String = "%.2f" format pawns

  def unary_- = copy(centipawns = -centipawns)
}

object Score {

  def apply(str: String): Option[Score] = parseIntOption(str) map Score.apply
}
