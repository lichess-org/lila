package lila.game

import akka.actor._
import akka.pattern.pipe
import shogi.format.{ Forsyth, Reader, Tags }
import shogi.{ Game => ShogiGame }
import scala.util.Success
import cats.data.NonEmptyList

import lila.common.Captcha
import lila.hub.actorApi.captcha._

final private class Captcher(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext)
    extends Actor {

  def receive = {

    case AnyCaptcha => sender() ! Impl.current

    case GetCaptcha(id: String) => Impl.get(id).pipeTo(sender()).unit

    case actorApi.NewCaptcha => Impl.refresh.unit

    case ValidCaptcha(id: String, solution: String) =>
      Impl.get(id).map(_ valid solution).pipeTo(sender()).unit
  }

  private object Impl {

    def get(id: String): Fu[Captcha] =
      find(id) match {
        case None    => getFromDb(id) map (c => (c | Captcha.default) ~ add)
        case Some(c) => fuccess(c)
      }

    def current = challenges.head

    def refresh =
      createFromDb andThen { case Success(Some(captcha)) =>
        add(captcha)
      }

    // Private stuff

    private val capacity   = 256
    private var challenges = NonEmptyList.one(Captcha.default)

    private def add(c: Captcha): Unit = {
      find(c.gameId) ifNone {
        challenges = NonEmptyList(c, challenges.toList take capacity)
      }
    }

    private def find(id: String): Option[Captcha] =
      challenges.find(_.gameId == id)

    private def createFromDb: Fu[Option[Captcha]] =
      findCheckmateInDb(10) flatMap {
        _.fold(findCheckmateInDb(1))(g => fuccess(g.some))
      } flatMap {
        _ ?? fromGame
      }

    private def findCheckmateInDb(distribution: Int): Fu[Option[Game]] =
      gameRepo findRandomStandardCheckmate distribution

    private def getFromDb(id: String): Fu[Option[Captcha]] =
      gameRepo game id flatMap { _ ?? fromGame }

    private def fromGame(game: Game): Fu[Option[Captcha]] =
      gameRepo getOptionUsis game.id map {
        _ flatMap { makeCaptcha(game, _) }
      }

    private def makeCaptcha(game: Game, moves: UsiMoves): Option[Captcha] =
      for {
        rewinded  <- rewind(moves)
        solutions <- solve(rewinded)
        moves = rewinded.situation.destinations map { case (from, dests) =>
          from.usiKey -> dests.mkString
        }
      } yield Captcha(game.id, fen(rewinded), rewinded.player.sente, solutions, moves = moves)

    // Won't find drop checkmates
    private def solve(game: ShogiGame): Option[Captcha.Solutions] =
      game.situation.moves.view
        .flatMap { case (_, moves) =>
          moves filter { move =>
            (move.after situationOf !game.player).checkMate
          }
        }
        .to(List) map { move =>
        s"${move.orig} ${move.dest}"
      } toNel

    private def rewind(moves: UsiMoves): Option[ShogiGame] =
      Reader
        .fromUsi(
          moves.dropRight(1),
          tags = Tags.empty
        )
        .valid
        .map(_.state)
        .toOption

    private def fen(game: ShogiGame): String = Forsyth.exportBoard(game.situation.board)
  }
}
