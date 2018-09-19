package lidraughts.analyse

import draughts.format.pdn.Glyph
import lidraughts.tree.Eval._

sealed trait Advice {
  def judgment: Advice.Judgment
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

  sealed abstract class Judgment(val glyph: Glyph, val name: String) {
    override def toString = name
    def isBlunder = this == Judgment.Blunder
  }
  object Judgment {
    object Inaccuracy extends Judgment(Glyph.MoveAssessment.dubious, "Inaccuracy")
    object Mistake extends Judgment(Glyph.MoveAssessment.mistake, "Mistake")
    object Blunder extends Judgment(Glyph.MoveAssessment.blunder, "Blunder")
    val all = List(Inaccuracy, Mistake, Blunder)
  }

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info) orElse WinAdvice(prev, info)
}

private[analyse] case class CpAdvice(
    judgment: Advice.Judgment,
    info: Info,
    prev: Info
) extends Advice

private[analyse] object CpAdvice {

  private val cpJudgments = List(
    300 -> Advice.Judgment.Blunder,
    100 -> Advice.Judgment.Mistake,
    50 -> Advice.Judgment.Inaccuracy
  )

  def apply(prev: Info, info: Info): Option[CpAdvice] = for {
    cp ← prev.cp map (_.ceiled.centipieces)
    infoCp ← info.cp map (_.ceiled.centipieces)
    delta = (infoCp - cp) |> { d => info.color.fold(-d, d) }
    judgment ← cpJudgments find { case (d, n) => d <= delta } map (_._2)
  } yield CpAdvice(judgment, info, prev)
}

private[analyse] sealed abstract class WinSequence(val desc: String)
private[analyse] case object WinDelayed$ extends WinSequence(
  desc = "Not the best winning sequence"
)
private[analyse] case object WinLost$ extends WinSequence(
  desc = "Lost forced winning sequence"
)
private[analyse] case object WinCreated$ extends WinSequence(
  desc = "Win is now unavoidable"
)

private[analyse] object WinSequence {
  def apply(prev: Option[Win], next: Option[Win]): Option[WinSequence] =
    (prev, next).some collect {
      case (None, Some(n)) if n.negative => WinCreated$
      case (Some(p), None) if p.positive => WinLost$
      case (Some(p), Some(n)) if p.positive && n.negative => WinLost$
      case (Some(p), Some(n)) if p.positive && n >= p && p <= Win(5) => WinDelayed$
    }
}
private[analyse] case class WinAdvice(
    sequence: WinSequence,
    judgment: Advice.Judgment,
    info: Info,
    prev: Info
) extends Advice
private[analyse] object WinAdvice {

  def apply(prev: Info, info: Info): Option[WinAdvice] = {
    def invertCp(cp: Cp) = cp invertIf info.color.black
    def invertWin(win: Win) = win invertIf info.color.black
    def prevCp = prev.cp.map(invertCp).??(_.centipieces)
    def nextCp = info.cp.map(invertCp).??(_.centipieces)
    WinSequence(prev.win map invertWin, info.win map invertWin) map { sequence =>
      import Advice.Judgment._
      val judgment = sequence match {
        case WinCreated$ if prevCp < -999 => Inaccuracy
        case WinCreated$ if prevCp < -700 => Mistake
        case WinCreated$ => Blunder
        case WinLost$ if nextCp > 999 => Inaccuracy
        case WinLost$ if nextCp > 700 => Mistake
        case WinLost$ => Blunder
        case WinDelayed$ => Inaccuracy
      }
      WinAdvice(sequence, judgment, info, prev)
    }
  }
}
