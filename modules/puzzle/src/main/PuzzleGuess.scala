package lila.puzzle

import chess.format.Fen
import chess.rating.glicko.Glicko
import scalalib.newtypes.OpaqueString

/* A position presented to the player, who must guess
 * whether it is a real puzzle (a winning tactic exists for the side to move)
 * or a normal game position.
 * Like puzzles, each position owns a glicko rating:
 * the more players guess it wrong, the higher its rating. */
case class PuzzleGuess(
    id: PuzzleGuess.Id,
    gameId: GameId,
    fen: Fen.Full, // position shown to the player, side to move = player pov
    puzzleId: Option[PuzzleId], // defined iff this is a real puzzle
    glicko: Glicko,
    plays: Int
):
  def isPuzzle = puzzleId.isDefined
  def color = fen.colorOrWhite

object PuzzleGuess:

  val idSize = 5

  opaque type Id = String
  object Id extends OpaqueString[Id]

  def makeId = Id(scalalib.ThreadLocalRandom.nextString(idSize))

  def toId(str: String): Option[Id] = (str.size == idSize).option(Id(str))

  // a player's guess, and the solving attempt if the position was a puzzle
  case class Round(
      id: Round.Id,
      guessWin: PuzzleWin, // did they identify puzzle-or-not correctly?
      solveWin: Option[PuzzleWin], // defined iff the position was a puzzle and correctly identified
      win: PuzzleWin, // overall: guessWin && solveWin.forall(_.yes)
      date: Instant
  )

  object Round:
    val idSep = ':'
    case class Id(userId: UserId, guessId: PuzzleGuess.Id):
      override def toString = s"${userId}$idSep${guessId}"

    object BSONFields:
      val id = "_id"
      val guessWin = "g"
      val solveWin = "s"
      val win = "w"
      val user = "u"
      val date = "d"

  // per-user rating & stats for this mode, separate from the puzzle perf
  case class Player(
      id: UserId,
      glicko: Glicko,
      runs: Int,
      wins: Int
  ):
    def intRating = chess.IntRating(glicko.rating.toInt)

  object Player:
    def default(userId: UserId) = Player(userId, lila.rating.Glicko.default, 0, 0)

    object BSONFields:
      val id = "_id"
      val glicko = "glicko"
      val runs = "runs"
      val wins = "wins"

  object BSONFields:
    val id = "_id"
    val gameId = "gameId"
    val fen = "fen"
    val puzzleId = "puzzleId"
    val glicko = "glicko"
    val plays = "plays"
