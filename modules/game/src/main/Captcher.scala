package lila.game

import scala.concurrent.Future

import akka.actor._
import akka.pattern.pipe
import chess.format.pgn.{ Tags, Sans }
import chess.format.{ Forsyth, pgn }
import chess.{ Game => ChessGame }
import scalaz.{ NonEmptyList, OptionT }
import scalaz.Validation.FlatMap._

import lila.common.Captcha
import lila.hub.actorApi.captcha._

// only works with standard chess (not chess960)
private final class Captcher extends Actor {

  def receive = {

    case AnyCaptcha => sender ! Impl.current

    case GetCaptcha(id: String) => Impl get id pipeTo sender

    case actorApi.NewCaptcha => Impl.refresh

    case ValidCaptcha(id: String, solution: String) =>
      Impl get id map (_ valid solution) pipeTo sender
  }

  private object Impl {

    def get(id: String): Fu[Captcha] = find(id) match {
      case None => getFromDb(id) map (c => (c | Captcha.default) ~ add)
      case Some(c) => fuccess(c)
    }

    def current = challenges.head

    def refresh = createFromDb onSuccess {
      case Some(captcha) => add(captcha)
    }

    // Private stuff

    private val capacity = 256
    private var challenges: NonEmptyList[Captcha] = NonEmptyList(Captcha.default)

    private def add(c: Captcha): Unit = {
      find(c.gameId) ifNone {
        challenges = NonEmptyList.nel(c, challenges.list take capacity)
      }
    }

    private def find(id: String): Option[Captcha] =
      challenges.list.find(_.gameId == id)

    private def createFromDb: Fu[Option[Captcha]] = {
      optionT(findCheckmateInDb(10) flatMap {
        _.fold(findCheckmateInDb(1))(g => fuccess(g.some))
      }) flatMap fromGame
    }.run

    private def findCheckmateInDb(distribution: Int): Fu[Option[Game]] =
      GameRepo findRandomStandardCheckmate distribution

    private def getFromDb(id: String): Fu[Option[Captcha]] =
      optionT(GameRepo game id) flatMap fromGame run

    private def fromGame(game: Game): OptionT[Fu, Captcha] =
      optionT(GameRepo getOptionPgn game.id) flatMap { makeCaptcha(game, _) }

    private def makeCaptcha(game: Game, moves: PgnMoves): OptionT[Fu, Captcha] =
      optionT(Future {
        for {
          rewinded ← rewind(game, moves)
          solutions ← solve(rewinded)
          moves = rewinded.situation.destinations map {
            case (from, dests) => from.key -> dests.mkString
          }
        } yield Captcha(game.id, fen(rewinded), rewinded.player.white, solutions, moves = moves)
      })

    private def solve(game: ChessGame): Option[Captcha.Solutions] =
      (game.situation.moves.flatMap {
        case (_, moves) => moves filter { move =>
          (move.after situationOf !game.player).checkMate
        }
      }(scala.collection.breakOut): List[chess.Move]) map { move =>
        s"${move.orig} ${move.dest}"
      } toNel

    private def rewind(game: Game, moves: PgnMoves): Option[ChessGame] =
      pgn.Reader.movesWithSans(
        moves,
        sans => Sans(safeInit(sans.value)),
        tags = Tags.empty
      ).flatMap(_.valid) map (_.state) toOption

    private def safeInit[A](list: List[A]): List[A] = list match {
      case x :: Nil => Nil
      case x :: xs => x :: safeInit(xs)
      case _ => Nil
    }

    private def fen(game: ChessGame): String = Forsyth >> game takeWhile (_ != ' ')
  }
}
