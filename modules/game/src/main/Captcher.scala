package lila.game

import akka.actor.*
import akka.pattern.pipe
import cats.data.NonEmptyList
import chess.format.pgn.{ Sans, Tags }
import chess.format.{ pgn, Forsyth }
import chess.{ Game as ChessGame }
import scala.util.Success

import lila.common.Captcha
import lila.hub.actorApi.captcha.*

// only works with standard chess (not chess960)
final private class Captcher(gameRepo: GameRepo)(using ec: scala.concurrent.ExecutionContext) extends Actor:

  def receive =

    case AnyCaptcha => sender() ! Impl.current

    case GetCaptcha(id) => Impl.get(id).pipeTo(sender()).unit

    case actorApi.NewCaptcha => Impl.refresh.unit

    case ValidCaptcha(id, solution) => Impl.get(id).map(_ valid solution).pipeTo(sender()).unit

  private object Impl:

    def get(id: GameId): Fu[Captcha] =
      find(id) match
        case None    => getFromDb(id) map (c => (c | Captcha.default) ~ add)
        case Some(c) => fuccess(c)

    def current = challenges.head

    def refresh =
      createFromDb andThen { case Success(Some(captcha)) =>
        add(captcha)
      }

    // Private stuff

    private val capacity   = 256
    private var challenges = NonEmptyList.one(Captcha.default)

    private def add(c: Captcha): Unit =
      if (find(c.gameId).isEmpty)
        challenges = NonEmptyList(c, challenges.toList take capacity)

    private def find(id: GameId): Option[Captcha] =
      challenges.find(_.gameId == id)

    private def createFromDb: Fu[Option[Captcha]] =
      findCheckmateInDb(10) orElse findCheckmateInDb(1) flatMap { _ ?? fromGame }

    private def findCheckmateInDb(distribution: Int): Fu[Option[Game]] =
      gameRepo findRandomStandardCheckmate distribution

    private def getFromDb(id: GameId): Fu[Option[Captcha]] =
      gameRepo game id flatMap { _ ?? fromGame }

    private def fromGame(game: Game): Fu[Option[Captcha]] =
      gameRepo getOptionPgn game.id map {
        _ flatMap { makeCaptcha(game, _) }
      }

    private def makeCaptcha(game: Game, moves: PgnMoves): Option[Captcha] =
      for {
        rewinded  <- rewind(moves)
        solutions <- solve(rewinded)
        moves = rewinded.situation.destinations map { case (from, dests) =>
          from.key -> dests.map(_.key).mkString
        }
      } yield Captcha(game.id, fenOf(rewinded), rewinded.player, solutions, moves = moves)

    private def solve(game: ChessGame): Option[Captcha.Solutions] =
      game.situation.moves.view
        .flatMap { case (_, moves) =>
          moves filter { move =>
            (move.after situationOf !game.player).checkMate
          }
        }
        .to(List) map { move =>
        s"${move.orig.key} ${move.dest.key}"
      } toNel

    private def rewind(moves: PgnMoves): Option[ChessGame] =
      pgn.Reader
        .movesWithSans(
          moves,
          sans => Sans(safeInit(sans.value)),
          tags = Tags.empty
        )
        .flatMap(_.valid) map (_.state) toOption

    private def safeInit[A](list: List[A]): List[A] =
      list match
        case _ :: Nil => Nil
        case x :: xs  => x :: safeInit(xs)
        case _        => Nil

    private def fenOf(game: ChessGame) = Forsyth exportBoard game.board
