package lila.tree

import chess.format.pgn.{ Comment, Glyph }
import chess.eval.*

sealed trait Advice:
  val judgment: Advice.Judgement
  val info: Info
  val prev: Info

  export info.{ ply, prevPly, prevMoveNumber, color, cp, mate }

  def makeComment(withEval: Boolean, withBestMove: Boolean): Comment = Comment:
    withEval.so(evalComment.so(c => s"($c) ")) +
      (this.match
        case MateAdvice(seq, _, _, _) => seq.desc
        case CpAdvice(judgment, _, _) => judgment.toString) + "." + withBestMove.so:
        info.variation.headOption.so: move =>
          s" $move was best."

  def evalComment: Option[String] =
    List(prev.evalComment, info.evalComment).flatten.mkString(" → ").some.filter(_.nonEmpty)

object Advice:

  enum Judgement(val glyph: Glyph, val name: String):
    case Inaccuracy extends Judgement(Glyph.MoveAssessment.dubious, "Inaccuracy")
    case Mistake extends Judgement(Glyph.MoveAssessment.mistake, "Mistake")
    case Blunder extends Judgement(Glyph.MoveAssessment.blunder, "Blunder")
    override def toString = name
    def isMistakeOrBlunder = this == Judgement.Mistake || this == Judgement.Blunder
  object Judgement:
    val all = values.toList

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info).orElse(MateAdvice(prev, info))

private[tree] case class CpAdvice(
    judgment: Advice.Judgement,
    info: Info,
    prev: Info
) extends Advice

private[tree] object CpAdvice:

  private val winningChanceJudgements = List(
    .3 -> Advice.Judgement.Blunder,
    .2 -> Advice.Judgement.Mistake,
    .1 -> Advice.Judgement.Inaccuracy
  )

  def apply(prev: Info, info: Info): Option[CpAdvice] =
    for
      cp <- prev.cp
      infoCp <- info.cp
      prevWinningChances = WinPercent.winningChances(cp)
      currentWinningChances = WinPercent.winningChances(infoCp)
      delta = (currentWinningChances - prevWinningChances).pipe(d => info.color.fold(-d, d))
      judgement <- winningChanceJudgements.find((d, _) => d <= delta).map(_._2)
    yield CpAdvice(judgement, info, prev)

sealed abstract private[tree] class MateSequence(val desc: String)
private[tree] case object MateCreated
    extends MateSequence(
      desc = "Checkmate is now unavoidable"
    )
private[tree] case object MateDelayed
    extends MateSequence(
      desc = "Not the best checkmate sequence"
    )
private[tree] case object MateLost
    extends MateSequence(
      desc = "Lost forced checkmate sequence"
    )

private[tree] object MateSequence:
  def apply(prev: Score, next: Score): Option[MateSequence] =
    (prev, next).some.collect:
      case (Score.Cp(_), Score.Mate(n)) if n.negative => MateCreated
      case (Score.Mate(p), Score.Cp(_)) if p.positive => MateLost
      case (Score.Mate(p), Score.Mate(n)) if p.positive && n.negative => MateLost
private[tree] case class MateAdvice(
    sequence: MateSequence,
    judgment: Advice.Judgement,
    info: Info,
    prev: Info
) extends Advice

private[tree] object MateAdvice:

  def apply(prev: Info, info: Info): Option[MateAdvice] =
    for
      prevScore <- prev.eval.score
      score <- info.eval.score
      prevPovScore = prevScore.invertIf(info.color.black)
      povScore = score.invertIf(info.color.black)
      prevPovCpOrZero = prevPovScore.cp.so(_.centipawns)
      povCpOrZero = povScore.cp.so(_.centipawns)
      sequence <- MateSequence(prevPovScore, povScore)
      judgement <- sequence match
        case MateCreated if prevPovCpOrZero < -999 => Some(Advice.Judgement.Inaccuracy)
        case MateCreated if prevPovCpOrZero < -700 => Some(Advice.Judgement.Mistake)
        case MateCreated => Some(Advice.Judgement.Blunder)
        case MateLost if povCpOrZero > 999 => Some(Advice.Judgement.Inaccuracy)
        case MateLost if povCpOrZero > 700 => Some(Advice.Judgement.Mistake)
        case MateLost => Some(Advice.Judgement.Blunder)
        case MateDelayed => None
    yield MateAdvice(sequence, judgement, info, prev)
