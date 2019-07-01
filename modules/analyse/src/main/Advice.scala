package lila.analyse

import chess.format.pgn.Glyph
import lila.tree.Eval._

sealed trait Advice {
  def judgment: Advice.Judgement
  def info: Info
  def prev: Info

  def ply = info.ply
  def turn = info.turn
  def color = info.color
  def cp = info.cp
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

  sealed abstract class Judgement(val glyph: Glyph, val name: String) {
    override def toString = name
    def isBlunder = this == Judgement.Blunder
  }
  object Judgement {
    object Inaccuracy extends Judgement(Glyph.MoveAssessment.dubious, "Inaccuracy")
    object Mistake extends Judgement(Glyph.MoveAssessment.mistake, "Mistake")
    object Blunder extends Judgement(Glyph.MoveAssessment.blunder, "Blunder")
    val all = List(Inaccuracy, Mistake, Blunder)
  }

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info) orElse MateAdvice(prev, info)
}

private[analyse] case class CpAdvice(
    judgment: Advice.Judgement,
    info: Info,
    prev: Info
) extends Advice

private[analyse] object CpAdvice {

  private val cpJudgements = List(
    300 -> Advice.Judgement.Blunder,
    100 -> Advice.Judgement.Mistake,
    50 -> Advice.Judgement.Inaccuracy
  )

  def apply(prev: Info, info: Info): Option[CpAdvice] = for {
    cp ← prev.cp map (_.ceiled.centipawns)
    infoCp ← info.cp map (_.ceiled.centipawns)
    delta = (infoCp - cp) |> { d => info.color.fold(-d, d) }
    judgment ← cpJudgements find { case (d, n) => d <= delta } map (_._2)
  } yield CpAdvice(judgment, info, prev)
}

private[analyse] sealed abstract class MateSequence(val desc: String)
private[analyse] case object MateCreated extends MateSequence(
  desc = "Checkmate is now unavoidable"
)
private[analyse] case object MateDelayed extends MateSequence(
  desc = "Not the best checkmate sequence"
)
private[analyse] case object MateLost extends MateSequence(
  desc = "Lost forced checkmate sequence"
)

private[analyse] object MateSequence {
  def apply(prev: Option[Mate], next: Option[Mate]): Option[MateSequence] =
    (prev, next).some collect {
      case (None, Some(n)) if n.negative => MateCreated
      case (Some(p), None) if p.positive => MateLost
      case (Some(p), Some(n)) if p.positive && n.negative => MateLost
    }
}
private[analyse] case class MateAdvice(
    sequence: MateSequence,
    judgment: Advice.Judgement,
    info: Info,
    prev: Info
) extends Advice
private[analyse] object MateAdvice {

  def apply(prev: Info, info: Info): Option[MateAdvice] = {
    def invertCp(cp: Cp) = cp invertIf info.color.black
    def invertMate(mate: Mate) = mate invertIf info.color.black
    def prevCp = prev.cp.map(invertCp).??(_.centipawns)
    def nextCp = info.cp.map(invertCp).??(_.centipawns)
    MateSequence(prev.mate map invertMate, info.mate map invertMate) flatMap { sequence =>
      import Advice.Judgement._
      val judgment: Option[Advice.Judgement] = sequence match {
        case MateCreated if prevCp < -999 => Option(Inaccuracy)
        case MateCreated if prevCp < -700 => Option(Mistake)
        case MateCreated => Option(Blunder)
        case MateLost if nextCp > 999 => Option(Inaccuracy)
        case MateLost if nextCp > 700 => Option(Mistake)
        case MateLost => Option(Blunder)
        case MateDelayed => None
      }
      judgment map { MateAdvice(sequence, _, info, prev) }
    }
  }
}
