package lila.core
package game

import _root_.chess.{ ByColor, Game as ChessGame, Rated, Status }
import scalalib.ThreadLocalRandom
import scalalib.model.Days

import lila.core.id.GameId

// Wrapper around newly created games. We do not know if the id is unique, yet.
case class NewGame(sloppy: Game):
  def withId(id: GameId): Game = sloppy.copy(id = id)
  def start: NewGame = NewGame(sloppy.start)

def newGame(
    chess: ChessGame,
    players: ByColor[Player],
    rated: Rated,
    source: Source,
    pgnImport: Option[PgnImport],
    daysPerTurn: Option[Days] = None,
    rules: Set[GameRule] = Set.empty
): NewGame = NewGame(newSloppy(chess, players, rated, source, pgnImport, daysPerTurn, rules))

def newSloppy(
    chess: ChessGame,
    players: ByColor[Player],
    rated: Rated,
    source: Source,
    pgnImport: Option[PgnImport],
    daysPerTurn: Option[Days] = None,
    rules: Set[GameRule] = Set.empty
): Game =
  val createdAt = nowInstant
  new Game(
    id = IdGenerator.uncheckedGame,
    players = players,
    chess = chess,
    status = Status.Created,
    daysPerTurn = daysPerTurn,
    rated = rated,
    metadata = newMetadata(source).copy(pgnImport = pgnImport, rules = rules),
    createdAt = createdAt,
    movedAt = createdAt
  )

trait IdGenerator:
  def game: Fu[GameId]
  def games(nb: Int): Fu[List[GameId]]
  def withUniqueId(sloppy: NewGame): Fu[Game]
object IdGenerator:
  def uncheckedGame: GameId = GameId(ThreadLocalRandom.nextString(GameId.size))
