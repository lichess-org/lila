package lidraughts.analyse

import draughts.format.pdn.Glyph
import lidraughts.tree.Eval._

sealed trait Advice {
  def judgment: Advice.Judgement
  def info: Info
  def prev: Info

  def ply = info.ply
  def turn = info.turn
  def color = info.color
  def cp = info.cp
  def win = info.win

  def makeComment(withEval: Boolean, withBestMove: Boolean): String =
    withEval.??(evalComment ?? { c => s"($c) " }) +
      (this match {
        case WinAdvice(seq, _, _, _) => seq.desc
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

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info) orElse WinAdvice(prev, info)
}

private[analyse] case class CpAdvice(
    judgment: Advice.Judgement,
    info: Info,
    prev: Info
) extends Advice

private[analyse] object CpAdvice {

  private def cpWinningChances(cp: Double): Double = 2 / (1 + Math.exp(-0.004 * cp)) - 1

  private val winningChanceJudgements = List(
    .3 -> Advice.Judgement.Blunder,
    .15 -> Advice.Judgement.Mistake,
    .075 -> Advice.Judgement.Inaccuracy
  )

  def apply(prev: Info, info: Info): Option[CpAdvice] = for {
    cp ← prev.cp map (_.ceiled.centipieces)
    infoCp ← info.cp map (_.ceiled.centipieces)
    prevWinningChances = cpWinningChances(cp)
    currentWinningChances = cpWinningChances(infoCp)
    delta = (currentWinningChances - prevWinningChances) |> { d => info.color.fold(-d, d) }
    judgement ← winningChanceJudgements find { case (d, n) => d <= delta } map (_._2)
  } yield CpAdvice(judgement, info, prev)
}

private[analyse] sealed abstract class WinSequence(val desc: String)
private[analyse] case object WinCreated extends WinSequence(
  desc = "Win is now unavoidable"
)
private[analyse] case object WinDelayed extends WinSequence(
  desc = "Not the best winning sequence"
)
private[analyse] case object WinLost extends WinSequence(
  desc = "Lost forced winning sequence"
)

private[analyse] object WinSequence {
  def apply(prev: Option[Win], next: Option[Win]): Option[WinSequence] =
    (prev, next).some collect {
      case (None, Some(n)) if n.negative => WinCreated
      case (Some(p), None) if p.positive => WinLost
      case (Some(p), Some(n)) if p.positive && n.negative => WinLost
    }
}
private[analyse] case class WinAdvice(
    sequence: WinSequence,
    judgment: Advice.Judgement,
    info: Info,
    prev: Info
) extends Advice
private[analyse] object WinAdvice {

  def apply(prev: Info, info: Info): Option[WinAdvice] = {
    def invertCp(cp: Cp) = cp invertIf info.color.black
    def invertWin(win: Win) = win invertIf info.color.black
    def prevCp = prev.cp.map(invertCp).??(_.centipieces)
    def nextCp = info.cp.map(invertCp).??(_.centipieces)
    WinSequence(prev.win map invertWin, info.win map invertWin) flatMap { sequence =>
      import Advice.Judgement._
      val judgment: Option[Advice.Judgement] = sequence match {
        case WinCreated if prevCp < -999 => Inaccuracy.some
        case WinCreated if prevCp < -600 => Mistake.some
        case WinCreated => Blunder.some
        case WinLost if nextCp > 999 => Inaccuracy.some
        case WinLost if nextCp > 600 => Mistake.some
        case WinLost => Blunder.some
        case WinDelayed => None
      }
      judgment map { WinAdvice(sequence, _, info, prev) }
    }
  }
}
