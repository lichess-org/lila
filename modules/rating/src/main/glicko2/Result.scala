package lila.rating.glicko2

// ASSUME score is between unit interval [0.0d, 1.0d]
class Result(opponent: Rating, score: Double):

  def getAdvantage(advantage: ColorAdvantage): ColorAdvantage = advantage.map(_ / 2.0d)

  def getScore(player: Rating): Double = score

  def getOpponent(player: Rating): Rating = opponent

class BinaryResult(val opponent: Rating, val win: Boolean) extends Result(opponent, if (win) 1.0d else 0.0d)

class DuelResult(val opponent: Rating, val score: Double, first: Boolean) extends Result(opponent, score):

  override def getAdvantage(advantage: ColorAdvantage): ColorAdvantage =
    if first then advantage.map(_ / 2.0d) else advantage.map(_ / 2.0d).negate
