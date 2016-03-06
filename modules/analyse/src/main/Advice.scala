package lila.analyse

import chess.format.Nag

sealed trait Advice {
  def nag: Nag
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
        case CpAdvice(nag, _, _)      => nag.toString
      }) + "." + {
        withBestMove ?? {
          info.variation.headOption ?? { move => s" Best move was $move." }
        }
      }

  def evalComment: Option[String] = {
    List(prev.evalComment, info.evalComment).flatten mkString " → "
  }.some filter (_.nonEmpty)
}

private[analyse] object Advice {

  def apply(prev: Info, info: Info): Option[Advice] = CpAdvice(prev, info) orElse MateAdvice(prev, info)
}

private[analyse] case class CpAdvice(
  nag: Nag,
  info: Info,
  prev: Info) extends Advice

private[analyse] object CpAdvice {

  private val cpNags = List(
    0.20 -> Nag.Blunder,
    0.10 -> Nag.Mistake,
    0.05 -> Nag.Inaccuracy)

  def apply(prev: Info, info: Info): Option[CpAdvice] = for {
    cp ← prev.score map (_.ceiled.centipawns)
    infoCp ← info.score map (_.ceiled.centipawns)
    a = -0.002409
    b = 1.001726
    c = 0.006963
    d = 2.386803
    before = a + (b-a)/(1 + math.exp(-c * (cp - d)));
    after  = a + (b-a)/(1 + math.exp(-c * (infoCp - d)));
    delta = (after - before) |> { d => info.color.fold(-d, d) }
    nag ← cpNags find { case (d, n) => d <= delta } map (_._2)
  } yield CpAdvice(nag, info, prev)
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
  nag: Nag,
  info: Info,
  prev: Info) extends Advice
private[analyse] object MateAdvice {

  def apply(prev: Info, info: Info): Option[MateAdvice] = {
    def reverse(m: Int) = info.color.fold(m, -m)
    def prevScore = reverse(prev.score ?? (_.centipawns))
    def nextScore = reverse(info.score ?? (_.centipawns))
    MateSequence(prev.mate map reverse, info.mate map reverse) map { sequence =>
      val nag = sequence match {
        case MateCreated if prevScore < -999 => Nag.Inaccuracy
        case MateCreated if prevScore < -700 => Nag.Mistake
        case MateCreated                     => Nag.Blunder
        case MateLost if nextScore > 999     => Nag.Inaccuracy
        case MateLost if nextScore > 700     => Nag.Mistake
        case MateLost                        => Nag.Blunder
        case MateDelayed                     => Nag.Inaccuracy
      }
      MateAdvice(sequence, nag, info, prev)
    }
  }
}
