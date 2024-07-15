package lila.rating.glicko2

trait Result:

  def getScore(player: Rating): Double

  def getOpponent(player: Rating): Rating

  def participated(player: Rating): Boolean

  def players: List[Rating]

// score from 0 (opponent wins) to 1 (player wins)
class FloatingResult(player: Rating, opponent: Rating, score: Float) extends Result:

  def getScore(p: Rating) = if p == player then score else 1 - score

  def getOpponent(p: Rating) = if p == player then opponent else player

  def participated(p: Rating) = p == player || p == opponent

  def players = List(player, opponent)

final class GameResult(winner: Rating, loser: Rating, isDraw: Boolean) extends Result:
  private val POINTS_FOR_WIN  = 1.0d
  private val POINTS_FOR_LOSS = 0.0d
  private val POINTS_FOR_DRAW = 0.5d

  def players = List(winner, loser)

  def participated(player: Rating) = player == winner || player == loser

  /** Returns the "score" for a match.
    *
    * @param player
    * @return
    *   1 for a win, 0.5 for a draw and 0 for a loss
    * @throws IllegalArgumentException
    */
  def getScore(player: Rating): Double =
    if isDraw then POINTS_FOR_DRAW
    else if winner == player then POINTS_FOR_WIN
    else if loser == player then POINTS_FOR_LOSS
    else throw new IllegalArgumentException("Player did not participate in match");

  def getOpponent(player: Rating) =
    if winner == player then loser
    else if loser == player then winner
    else throw new IllegalArgumentException("Player did not participate in match");

  override def toString = s"$winner vs $loser = $isDraw"
