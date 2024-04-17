package lila.core
package game

import scala.concurrent.ExecutionContext
import scalalib.model.Days
import scalalib.ThreadLocalRandom
import _root_.chess.{ Color, Mode, ByColor, Status, Game as ChessGame }

import lila.core.id.{ GameId, GamePlayerId }
import lila.core.user.WithPerf

// Wrapper around newly created games. We do not know if the id is unique, yet.
case class NewGame(sloppy: Game):
  def withId(id: GameId): Game = sloppy.withId(id)
  def start: NewGame           = NewGame(sloppy.start)

  // Forward methods as needed, but do not expose the unchecked id.
  export sloppy.{ variant, finished, winnerColor, outcome, status, history }

def newGame(
    chess: ChessGame,
    players: ByColor[Player],
    mode: Mode,
    source: Source,
    pgnImport: Option[PgnImport],
    daysPerTurn: Option[Days] = None,
    rules: Set[GameRule] = Set.empty
): NewGame =
  val createdAt = nowInstant
  NewGame:
    new Game(
      id = IdGenerator.uncheckedGame,
      players = players,
      chess = chess,
      status = Status.Created,
      daysPerTurn = daysPerTurn,
      mode = mode,
      metadata = newMetadata(source).copy(pgnImport = pgnImport, rules = rules),
      createdAt = createdAt,
      movedAt = createdAt
    )

trait IdGenerator:
  def game: Fu[GameId]
  def games(nb: Int): Fu[Set[GameId]]
  def withUniqueId(sloppy: NewGame): Fu[Game]
object IdGenerator:
  def uncheckedGame: GameId = GameId(ThreadLocalRandom.nextString(GameId.size))
