package lila.game

import akka.actor._
import akka.pattern.pipe
import shogi.format.{ Reader, Tags }
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

    private def makeCaptcha(game: Game, usis: Usis): Option[Captcha] =
      for {
        rewinded  <- rewind(usis)
        solutions <- solve(rewinded)
        moves = rewinded.situation.moveDestinations map { case (from, dests) =>
          from.key -> dests.mkString
        }
      } yield Captcha(game.id, sfen(rewinded), rewinded.color.sente, solutions, moves = moves)

    // Not looking for drop checkmates or checking promotions
    private def solve(game: ShogiGame): Option[Captcha.Solutions] =
      game.situation
        .moveActorsOf(game.situation.color)
        .view
        .flatMap { case moveActor =>
          moveActor.toUsis filter { usi =>
            game.situation(usi).toOption.fold(false)(_.checkmate)
          }
        }
        .to(List) map { usi =>
        s"${usi.orig} ${usi.dest}"
      } toNel

    private def rewind(moves: Usis): Option[ShogiGame] =
      Reader
        .fromUsi(
          moves.dropRight(1),
          none,
          shogi.variant.Standard,
          Tags.empty
        )
        .valid
        .map(_.state)
        .toOption

    // only board
    private def sfen(game: ShogiGame): String = game.situation.toSfen.value.split(' ').take(1).mkString(" ")
  }
}
