package lidraughts.game

import scala.concurrent.Future

import akka.actor._
import akka.pattern.pipe
import draughts.format.pdn.{ Tags, Sans }
import draughts.format.{ Forsyth, pdn }
import draughts.{ DraughtsGame, Move, Situation }
import scalaz.{ NonEmptyList, OptionT }
import scalaz.Validation.FlatMap._

import lidraughts.common.Captcha
import lidraughts.hub.actorApi.captcha._

// only works with standard draughts (not chess960)
private final class Captcher extends Actor {

  def receive = {

    case AnyCaptcha => sender ! Impl.current

    case GetCaptcha(id: String) => Impl get id pipeTo sender

    case actorApi.NewCaptcha => Impl.refresh

    case ValidCaptcha(id: String, solution: String) =>
      Impl get id map (_ valid solution) pipeTo sender
  }

  private object Impl {

    def get(id: String): Fu[Captcha] =
      find(id) match {
        case None => getFromDb(id) map (c => (c | Captcha.default) ~ add)
        case Some(c) => fuccess(c)
      }

    def current = challenges.head

    def refresh = createFromDb onSuccess {
      case Some(captcha) => add(captcha)
    }

    // Private stuff

    private val capacity = 512
    private var challenges: NonEmptyList[Captcha] = NonEmptyList(Captcha.default)

    private def add(c: Captcha): Unit =
      find(c.gameId) ifNone {
        challenges = NonEmptyList.nel(c, challenges.list take capacity)
      }

    private def find(id: String): Option[Captcha] =
      challenges.list.find(_.gameId == id)

    private def createFromDb: Fu[Option[Captcha]] =
      {
        optionT(findFinishedInDb(50) flatMap {
          _.fold(findFinishedInDb(1))(g => fuccess(g.some))
        }) flatMap fromGame
      }.run

    private def findFinishedInDb(distribution: Int): Fu[Option[Game]] =
      GameRepo.findAnyRandomFinished(distribution, 20)

    private def getFromDb(id: String): Fu[Option[Captcha]] =
      optionT(GameRepo game id) flatMap fromGame run

    private def fromGame(game: Game): OptionT[Fu, Captcha] =
      optionT(GameRepo getOptionPdn game.id) flatMap { makeCaptcha(game, _) }

    private def makeCaptcha(game: Game, moves: PdnMoves): OptionT[Fu, Captcha] =
      optionT(Future {
        for {
          rewinded ← rewind(game, moves)
          situation ← findCapture(rewinded)
          solutions ← solve(situation)
          moves = situation.allCaptureDestinations map {
            case (from, dests) => from.key -> dests.mkString
          }
        } yield Captcha(game.id, fen(situation), situation.color.white, solutions, moves = moves)
      })

    private def solve(situation: Situation): Option[Captcha.Solutions] =
      (situation.validMoves.flatMap {
        case (_, moves) =>
          moves filter { _.captures }
      }(scala.collection.breakOut): List[draughts.Move]) map { move =>
        s"${move.orig} ${move.dest}"
      } toNel

    private def findCapture(moves: List[Move]): Option[Situation] =
      moves.reverse.find(
        _.situationBefore.allCaptures.foldLeft(List[Int]())(
          (lengths, captures) =>
            captures._2.foldLeft(lengths)(
              (lengthsMove, capturesMove) => {
                val captLen = capturesMove.capture.get.length
                if (!lengthsMove.contains(captLen)) captLen :: lengthsMove else lengthsMove
              }
            )
        ).lengthCompare(1) > 0
      ).fold(none[Situation])(_.situationBefore.some)

    private def rewind(game: Game, moves: PdnMoves): Option[List[Move]] =
      pdn.Reader.movesWithSans(
        moves,
        sans => Sans(safeInit(sans.value)),
        tags = Tags.empty
      ).flatMap(_.valid) map (_.moves) toOption

    private def safeInit[A](list: List[A]): List[A] = list match {
      case x :: Nil => Nil
      case x :: xs => x :: safeInit(xs)
      case _ => Nil
    }

    private def fen(game: DraughtsGame): String = Forsyth >> game takeWhile (_ != ' ')
    private def fen(situation: Situation): String = Forsyth >> situation takeWhile (_ != ' ')
  }
}
