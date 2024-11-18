package lila.rating.glicko2

import chess.{ Color, White }

// ASSUME score is between unit interval [0.0d, 1.0d]
class Result(opponent: Rating, score: Double):

  def getAdvantage(advantage: ColorAdvantage): ColorAdvantage = advantage.map(_ / 2.0d)

  def getScore(player: Rating): Double = score

  def getOpponent(player: Rating): Rating = opponent

class BinaryResult(val opponent: Rating, val win: Boolean)
    extends Result(opponent, if win then 1.0d else 0.0d)

class DuelResult(val opponent: Rating, val score: Double, color: Color) extends Result(opponent, score):

  override def getAdvantage(advantage: ColorAdvantage): ColorAdvantage =
    if color == White then advantage.map(_ / 2.0d) else advantage.map(_ / 2.0d).negate
