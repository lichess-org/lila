package lila
package game

import akka.actor._
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import akka.util.Duration
import akka.util.duration._
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

import chess.Color

final class Featured(
    gameRepo: GameRepo) {

  import Featured._

  def one: Option[DbGame] = Await.result(
    actor ? GetOne mapTo manifest[Option[DbGame]],
    atMost)

  private val atMost = 2.second
  private implicit val timeout = Timeout(atMost)

  private val actor = Akka.system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {
      case GetOne ⇒ sender ! getOne
    }

    private def getOne = oneId flatMap fetch filter valid orElse {
      feature ~ { o ⇒ oneId = o map (_.id) }
    }

    private def fetch(id: String): Option[DbGame] =
      gameRepo.game(id).unsafePerformIO

    private def valid(game: DbGame) = game.isBeingPlayed

    private def feature: Option[DbGame] =
      Featured.best(gameRepo.featuredCandidates.unsafePerformIO)
  }))
}

object Featured {

  case object GetOne

  def best(games: List[DbGame]) = (games sortBy score).headOption

  private def score(game: DbGame): Float = heuristics map {
    case (fn, coefficient) ⇒ heuristicBox(fn(game)) * coefficient
  } sum

  private type Heuristic = DbGame ⇒ Float
  private val heuristicBox = box(0 to 1) _
  private val eloBox = box(1200 to 2000) _
  private val timeBox = box(60 to 300) _
  private val turnBox = box(1 to 20) _

  private val heuristics: List[(Heuristic, Float)] = List(
    eloHeuristic(Color.White) -> 1,
    eloHeuristic(Color.Black) -> 1,
    speedHeuristic -> 1,
    progressHeuristic -> 1)

  private def eloHeuristic(color: Color): Heuristic = game ⇒
    eloBox(game.player(color).eloEstimation)

  private def speedHeuristic: Heuristic = game ⇒
    1 - timeBox(game.estimateTotalTime)

  private def progressHeuristic: Heuristic = game ⇒
    1 - turnBox(game.turns)

  // boxes and reduce to 0..1 range
  private def box(in: Range.Inclusive)(v: Float): Float =
    math.max(in.start, math.min(0, in.end)) - in.start / (in.end - in.start)
}
