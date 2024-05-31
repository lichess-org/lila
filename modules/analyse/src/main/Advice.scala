package lila.analyse

import shogi.format.Glyph
import lila.tree.Eval._
import scala.util.chaining._

sealed trait Advice {
  def judgment: Advice.Judgement
  def info: Info
  def prev: Info

  def ply   = info.ply
  def turn  = info.turn
  def color = info.color
  def cp    = info.cp
  def mate  = info.mate

  def makeComment(withEval: Boolean, withBestMove: Boolean): String =
    withEval.??(evalComment ?? { c =>
      s"($c) "
    }) +
      (this match {
        case MateAdvice(seq, _, _, _) => seq.desc
        case CpAdvice(judgment, _, _) => judgment.toString
      }) + "." + {
        withBestMove ?? {
          info.variation.headOption ?? { usi =>
            s" Best ${if (usi.contains("*")) "drop" else "move"} was [usi:${ply - 1}.$usi]"
          }
        }
      }

  def evalComment: Option[String] = {
    List(prev.evalComment, info.evalComment).flatten mkString " → "
  }.some filter (_.nonEmpty)

}

object Advice {

  sealed abstract class Judgement(val glyph: Glyph, val name: String) {
    override def toString = name
    def isBlunder         = this == Judgement.Blunder
  }
  object Judgement {
    object Inaccuracy extends Judgement(Glyph.MoveOrDropAssessment.dubious, "Inaccuracy")
    object Mistake    extends Judgement(Glyph.MoveOrDropAssessment.mistake, "Mistake")
    object Blunder    extends Judgement(Glyph.MoveOrDropAssessment.blunder, "Blunder")
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

  private def cpWinningChances(cp: Double): Double = 2 / (1 + Math.exp(-0.0007 * cp)) - 1

  private val winningChanceJudgements = List(
    .3 -> Advice.Judgement.Blunder,
    .2 -> Advice.Judgement.Mistake,
    .1 -> Advice.Judgement.Inaccuracy
  )

  def apply(prev: Info, info: Info): Option[CpAdvice] =
    for {
      cp     <- prev.cp map (_.ceiled.centipawns)
      infoCp <- info.cp map (_.ceiled.centipawns)
      prevWinningChances    = cpWinningChances(cp)
      currentWinningChances = cpWinningChances(infoCp)
      delta = (currentWinningChances - prevWinningChances) pipe { d =>
        info.color.fold(-d, d)
      }
      judgement <- winningChanceJudgements find { case (d, _) => d <= delta } map (_._2)
    } yield CpAdvice(judgement, info, prev)
}

sealed abstract private[analyse] class MateSequence(val desc: String)
private[analyse] case object MateCreated
    extends MateSequence(
      desc = "Checkmate is now unavoidable"
    )
private[analyse] case object MateDelayed
    extends MateSequence(
      desc = "Not the best checkmate sequence"
    )
private[analyse] case object MateLost
    extends MateSequence(
      desc = "Lost forced checkmate sequence"
    )

private[analyse] object MateSequence {
  def apply(prev: Option[Mate], next: Option[Mate]): Option[MateSequence] =
    (prev, next).some collect {
      case (None, Some(n)) if n.negative                  => MateCreated
      case (Some(p), None) if p.positive                  => MateLost
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
    def invertCp(cp: Cp)       = cp invertIf info.color.gote
    def invertMate(mate: Mate) = mate invertIf info.color.gote
    def prevCp                 = prev.cp.map(invertCp).??(_.centipawns)
    def nextCp                 = info.cp.map(invertCp).??(_.centipawns)
    MateSequence(prev.mate map invertMate, info.mate map invertMate) flatMap { sequence =>
      import Advice.Judgement._
      val judgment: Option[Advice.Judgement] = sequence match {
        case MateCreated if prevCp < -4000 => Option(Inaccuracy)
        case MateCreated if prevCp < -1000 => Option(Mistake)
        case MateCreated                   => Option(Blunder)
        case MateLost if nextCp > 4000     => Option(Inaccuracy)
        case MateLost if nextCp > 1000     => Option(Mistake)
        case MateLost                      => Option(Blunder)
        case MateDelayed                   => None
      }
      judgment map { MateAdvice(sequence, _, info, prev) }
    }
  }
}
