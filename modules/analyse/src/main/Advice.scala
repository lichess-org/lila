package lila.analyse

import chess.format.pgn.Glyph

sealed trait Advice {
  def judgment: Advice.Judgment
  def info: Info
  def prev: Info

  def ply = info.ply
  def turn = info.turn
  def color = info.color
  def score = info.score
  def mate = info.mate

  def makeComment(withEval: Boolean, withBestMove: Boolean): String =
    withEval.??(evalComment ?? { c => s"($c) " }) +
      (this match {
        case MateAdvice(seq, _, _, _) => seq.desc
        case CpAdvice(judgment, _, _) => judgment.toString
      }) + "." + {
        withBestMove ?? {
          info.variation.headOption ?? { move => s" Best move was $move." }
        }
      }

  def evalComment: Option[String] = {
    List(prev.evalComment, info.evalComment).flatten mkString " → "
  }.some filter (_.nonEmpty)
}

object Advice {

  sealed abstract class Judgment(val glyph: Glyph, val name: String) {
    override def toString = name
    def isBlunder = this == Judgment.Blunder
  }
  object Judgment {
    object Inaccuracy extends Judgment(Glyph.MoveAssessment.questionable, "Inaccuracy")
    object Mistake extends Judgment(Glyph.MoveAssessment.mistake, "Mistake")
    object Blunder extends Judgment(Glyph.MoveAssessment.blunder, "Blunder")
    val all = List(Inaccuracy, Mistake, Blunder)
  }

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info) orElse MateAdvice(prev, info)
}

private[analyse] case class CpAdvice(
  judgment: Advice.Judgment,
  info: Info,
  prev: Info) extends Advice

private[analyse] object CpAdvice {

  private val cpJudgments = List(
    300 -> Advice.Judgment.Blunder,
    100 -> Advice.Judgment.Mistake,
    50 -> Advice.Judgment.Inaccuracy)

  def apply(prev: Info, info: Info): Option[CpAdvice] = for {
    cp ← prev.score map (_.ceiled.centipawns)
    infoCp ← info.score map (_.ceiled.centipawns)
    delta = (infoCp - cp) |> { d => info.color.fold(-d, d) }
    judgment ← cpJudgments find { case (d, n) => d <= delta } map (_._2)
  } yield CpAdvice(judgment, info, prev)
}

private[analyse] sealed abstract class MateSequence(val desc: String)
private[analyse] case object MateDelayed extends MateSequence(
  desc = "Not the best checkmate sequence")
private[analyse] case object MateLost extends MateSequence(
  desc = "Lost forced checkmate sequence")
private[analyse] case object MateCreated extends MateSequence(
  desc = "Checkmate is now unavoidable")

private[analyse] object MateSequence {
  def apply(prev: Option[Int], next: Option[Int]): Option[MateSequence] =
    (prev, next).some collect {
      case (None, Some(n)) if n < 0                        => MateCreated
      case (Some(p), None) if p > 0                        => MateLost
      case (Some(p), Some(n)) if (p > 0) && (n < 0)        => MateLost
      case (Some(p), Some(n)) if p > 0 && n >= p && p <= 5 => MateDelayed
    }
}
private[analyse] case class MateAdvice(
  sequence: MateSequence,
  judgment: Advice.Judgment,
  info: Info,
  prev: Info) extends Advice
private[analyse] object MateAdvice {

  def apply(prev: Info, info: Info): Option[MateAdvice] = {
    def reverse(m: Int) = info.color.fold(m, -m)
    def prevScore = reverse(prev.score ?? (_.centipawns))
    def nextScore = reverse(info.score ?? (_.centipawns))
    MateSequence(prev.mate map reverse, info.mate map reverse) map { sequence =>
      import Advice.Judgment._
      val judgment = sequence match {
        case MateCreated if prevScore < -999 => Inaccuracy
        case MateCreated if prevScore < -700 => Mistake
        case MateCreated                     => Blunder
        case MateLost if nextScore > 999     => Inaccuracy
        case MateLost if nextScore > 700     => Mistake
        case MateLost                        => Blunder
        case MateDelayed                     => Inaccuracy
      }
      MateAdvice(sequence, judgment, info, prev)
    }
  }
}
