package lila.rating.glicko2

trait Result:

  def getAdvantage(advantage: Double, player: Rating): Double

  def getScore(player: Rating): Double

  def getOpponent(player: Rating): Rating

  def participated(player: Rating): Boolean

  def players: List[Rating]

class BinaryResult(first: Rating, second: Rating, score: Boolean) extends Result:
  private val POINTS_FOR_WIN  = 1.0d
  private val POINTS_FOR_LOSS = 0.0d

  def getAdvantage(advantage: Double, p: Rating) = 0.0d

  def getScore(p: Rating) = if p == first then if score then POINTS_FOR_WIN else POINTS_FOR_LOSS
  else if score then POINTS_FOR_LOSS
  else POINTS_FOR_WIN

  def getOpponent(p: Rating) = if p == first then second else first

  def participated(p: Rating) = p == first || p == second

  def players = List(first, second)

final class GameResult(first: Rating, second: Rating, outcome: Option[Boolean]) extends Result:
  private val POINTS_FOR_WIN  = 1.0d
  private val POINTS_FOR_DRAW = 0.5d
  private val POINTS_FOR_LOSS = 0.0d

  def players = List(first, second)

  def participated(player: Rating) = player == first || player == second

  def getAdvantage(advantage: Double, player: Rating) =
    if player == first then advantage / 2.0d else -advantage / 2.0d

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
