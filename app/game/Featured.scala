package lila
package game

import akka.actor._
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import scala.concurrent.Duration
import scala.concurrent.duration._
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scalaz.effects._

import socket.ChangeFeatured
import chess.Color

final class Featured(
    gameRepo: GameRepo,
    lobbyHubName: String) {

  import Featured._

  def one: Future[Option[DbGame]] =
    actor ? GetOne mapTo manifest[Option[DbGame]]

  private implicit val timeout = Timeout(2 seconds)
  private lazy val lobbyRef = Akka.system.actorFor("/user/" + lobbyHubName)

  private val actor = Akka.system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {
      case GetOne ⇒ sender ! getOne
    }

    private def getOne = oneId flatMap fetch filter valid orElse {
      feature ~ { newOne ⇒
        oneId = newOne map (_.id)
        newOne foreach { game ⇒
          lobbyRef ! ChangeFeatured(views.html.game.featuredJsNoCtx(game).toString)
        }
      }
    }

    private def fetch(id: String): Option[DbGame] =
      gameRepo.game(id).unsafePerformIO

    private def valid(game: DbGame) = game.isBeingPlayed

    private def feature: Option[DbGame] = Featured best {
      gameRepo.featuredCandidates.unsafePerformIO filter valid
    }
  }))

  Akka.system.scheduler.schedule(5.seconds, 2.seconds, actor, GetOne)
}

object Featured {

  case object GetOne

  def best(games: List[DbGame]) = (games sortBy score).lastOption

  def score(game: DbGame): Float = heuristics map {
    case (fn, coefficient) ⇒ heuristicBox(fn(game)) * coefficient
  } sum

  private type Heuristic = DbGame ⇒ Float
  private val heuristicBox = box(0 to 1) _
  private val eloBox = box(1000 to 2000) _
  private val timeBox = box(60 to 300) _
  private val turnBox = box(1 to 21) _

  private val heuristics: List[(Heuristic, Float)] = List(
    eloHeuristic(Color.White) -> 1,
    eloHeuristic(Color.Black) -> 1,
    speedHeuristic -> 1,
    progressHeuristic -> 0.5f)

  def eloHeuristic(color: Color): Heuristic = game ⇒
    eloBox(game.player(color).elo | 1000)

  def speedHeuristic: Heuristic = game ⇒
    1 - timeBox(game.estimateTotalTime)

  def progressHeuristic: Heuristic = game ⇒
    1 - turnBox(game.turns)

  // boxes and reduce to 0..1 range
  def box(in: Range.Inclusive)(v: Float): Float =
    (math.max(in.start, math.min(v, in.end)) - in.start) / (in.end - in.start).toFloat
}
