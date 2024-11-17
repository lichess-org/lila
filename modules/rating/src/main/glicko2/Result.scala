package lila.rating.glicko2

trait Result:

  val first: Rating
  val second: Rating

  def getAdvantage(advantage: ColorAdvantage, player: Rating): ColorAdvantage =
    if player == first then advantage.map(_ / 2.0d) else advantage.map(_ / 2.0d).negate

  def getScore(player: Rating): Double

  def getOpponent(player: Rating) = if player == first then second else first

  def participated(player: Rating): Boolean = player == first || player == second

  private def players: List[Rating] = List(first, second)

final class BinaryResult(val first: Rating, val second: Rating, score: Boolean) extends Result:
  private val POINTS_FOR_WIN  = 1.0d
  private val POINTS_FOR_LOSS = 0.0d

  def getScore(p: Rating): Double =
    if p == first then if score then POINTS_FOR_WIN else POINTS_FOR_LOSS
    else if score then POINTS_FOR_LOSS
    else POINTS_FOR_WIN

// ASSUME score is between unit interval [0.0d, 1.0d]
final class FloatingResult(val first: Rating, val second: Rating, score: Double) extends Result:
  private val ONE_HUNDRED_PCT = 1.0d

  def getScore(p: Rating): Double =
    if p == first then score else ONE_HUNDRED_PCT - score

final class DuelResult(val first: Rating, val second: Rating, outcome: Option[Boolean]) extends Result:
  private val POINTS_FOR_WIN  = 1.0d
  private val POINTS_FOR_DRAW = 0.5d
  private val POINTS_FOR_LOSS = 0.0d

  /** Returns the "score" for a match.
    *
    * @return
    *   1 for a first player win, 0.5 for a draw and 0 for a first player loss
    * @throws IllegalArgumentException
    */
  def getScore(player: Rating): Double = outcome match
    case Some(true)  => if player == first then POINTS_FOR_WIN else POINTS_FOR_LOSS
    case Some(false) => if player == first then POINTS_FOR_LOSS else POINTS_FOR_WIN
    case _           => POINTS_FOR_DRAW

  override def toString = s"$first vs $second = $outcome"
