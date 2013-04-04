package lila.game

import lila.db.api._
import tube.gameTube

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.templates.Html

import chess.Color

private[game] final class Featured(
    lobbyActor: ActorRef,
    rendererActor: ActorRef,
    system: ActorSystem) {

  import Featured._
  import makeTimeout.large

  def one: Future[Option[Game]] = actor ? GetOne mapTo manifest[Option[Game]]

  private val actor = system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {
      case GetOne ⇒ sender ! getOne
    }

    // this function must block to make the actor wait for the result
    // before accepting new messages
    private def getOne: Option[Game] =
      oneId flatMap fetch filter valid orElse {
        feature ~ { newOne ⇒
          oneId = newOne map (_.id)
          newOne foreach { game ⇒
            rendererActor ? actorApi.RenderFeaturedJs(game) onSuccess {
              case html: Html ⇒ lobbyActor ! actorApi.ChangeFeatured(html)
            }
          }
        }
      }

    private def fetch(id: String): Option[Game] = ($find byId id).await

    private def valid(game: Game) = game.isBeingPlayed

    private def feature: Option[Game] = Featured best {
      GameRepo.featuredCandidates.await filter valid
    }
  }))

  system.scheduler.schedule(5.seconds, 2.seconds, actor, GetOne)
}

object Featured {

  case object GetOne

  def best(games: List[Game]) = (games sortBy score).lastOption

  def score(game: Game): Float = heuristics map {
    case (fn, coefficient) ⇒ heuristicBox(fn(game)) * coefficient
  } sum

  private type Heuristic = Game ⇒ Float
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
