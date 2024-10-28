package lila.rating.glicko2

trait Result:

  val first: Rating
  val second: Rating

  def getAdvantage(advantage: Double, player: Rating): Double =
    if player == first then advantage / 2.0d else -advantage / 2.0d

  def getScore(player: Rating): Double

  def getOpponent(player: Rating): Rating

  def participated(player: Rating): Boolean = player == first || player == second

  def players: List[Rating] = List(first, second)

class BinaryResult(val first: Rating, val second: Rating, score: Boolean) extends Result:
  private val POINTS_FOR_WIN  = 1.0d
  private val POINTS_FOR_LOSS = 0.0d

  def getScore(p: Rating) = if p == first then if score then POINTS_FOR_WIN else POINTS_FOR_LOSS
  else if score then POINTS_FOR_LOSS
  else POINTS_FOR_WIN

  def getOpponent(p: Rating) = if p == first then second else first

final class GameResult(val first: Rating, val second: Rating, outcome: Option[Boolean]) extends Result:
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

  def getOpponent(player: Rating) =
    if first == player then second
    else if second == player then first
    else throw new IllegalArgumentException("Player did not participate in match");

  override def toString = s"$first vs $second = $outcome"
