package lila
package site

import game._
import chess.{ Game, Color }
import chess.format.Forsyth
import chess.format.pgn

import scalaz.{ NonEmptyList, NonEmptyLists }
import scala.collection.mutable
import scala.util.Random
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

// only works with standard chess (not chess960)
final class Captcha(gameRepo: GameRepo, pgnRepo: PgnRepo) {

  import Captcha._

  def create: Challenge = Cache.create

  def get(id: String): Challenge = Cache get id

  private object Cache extends NonEmptyLists {
    val timeout = 10 seconds
    val history = 1000
    private var challenges: NonEmptyList[Challenge] = nel(createFromDb | Challenge.default)
    private var date = DateTime.now

    def create = { refresh; current }

    def get(id: String) = find(id) | { getFromDb(id).err ~ add }

    private def current = challenges.head

    private def refresh {
      if (date < DateTime.now - timeout) {
        createFromDb.toOption foreach add
      }
    }

    private def add(c: Challenge) {
      find(c.gameId) ifNone { challenges = nel(c, challenges.list take history) }
      date = DateTime.now
    }

    private def find(id: String) = challenges.list.find(_.gameId == id)
  }

  private def createFromDb: Valid[Challenge] = {
    val gameOption = findCheckmateInDb(100) orElse findCheckmateInDb(1)
    for {
      game ← gameOption toValid "No checkmate available in db"
      pgnString = getGamePgn(game.id)
      challenge ← makeChallenge(game, pgnString)
    } yield challenge
  }

  private def findCheckmateInDb(distribution: Int) =
    gameRepo.findRandomStandardCheckmate(distribution).unsafePerformIO

  private def getGamePgn(id: String) = (pgnRepo get id).unsafePerformIO

  private def getFromDb(id: String): Valid[Challenge] = {
    val gameOption = (gameRepo game id).unsafePerformIO
    for {
      game ← gameOption toValid "No such game: " + id
      pgnString = getGamePgn(game.id)
      challenge ← makeChallenge(game, pgnString)
    } yield challenge
  }

  private def makeChallenge(game: DbGame, pgnString: String): Valid[Challenge] = for {
    rewinded ← rewind(game, pgnString)
    solutions ← solve(rewinded)
  } yield Challenge(game.id, fen(rewinded), rewinded.player, solutions)

  private def solve(game: Game): Valid[Solutions] =
    mateMoves(game).toNel toValid "No solution found for: " + game

  private def mateMoves(game: Game): List[String] =
    game.situation.moves.toList flatMap {
      case (_, moves) ⇒ moves filter { move ⇒
        (move.after situationOf !game.player).checkMate
      }
    } map (_.notation)

  private def rewind(game: DbGame, pgnString: String): Valid[Game] =
    pgn.Reader.withSans(pgnString, _.init) map (_.game) mapFail failInfo(game)

  private def fen(game: Game): String = Forsyth >> game takeWhile (_ != ' ')

  private def failInfo(game: DbGame) =
    (failures: Failures) ⇒ "Rewind %s".format(game.id) <:: failures
}

object Captcha {

  type Solutions = NonEmptyList[String]

  case class Challenge(
      gameId: String,
      fen: String,
      color: Color,
      solutions: Solutions) {

    def valid(solution: String) = solutions.list contains solution
  }

  object Challenge {

    def default = Challenge(
      gameId = "00000000",
      fen = "1k3b1r/r5pp/pNQppq2/2p5/4P3/P3B3/1P3PPP/n4RK1",
      color = Color.White,
      solutions = nel("c6 c8", Nil))
  }
}
