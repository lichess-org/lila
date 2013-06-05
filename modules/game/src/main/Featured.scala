package lila.game

import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }

import akka.actor._
import akka.pattern.ask
import Featured._
import play.api.Play.current
import play.api.templates.Html

import chess.Color
import lila.db.api._
import makeTimeout.large
import tube.gameTube

final class Featured(
    lobbySocket: lila.hub.ActorLazyRef,
    rendererActor: lila.hub.ActorLazyRef,
    system: ActorSystem) {

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
              case html: Html ⇒ lobbySocket ! actorApi.ChangeFeatured(html)
            }
          }
        }
      }

    private def fetch(id: String): Option[Game] = ($find byId id).await

    private def valid(game: Game) = game.isBeingPlayed

    private def feature: Option[Game] = Featured sort {
      GameRepo.featuredCandidates.await filter valid
    } lastOption
  }))
}

object Featured {

  private case object GetOne

  def sort(games: List[Game]): List[Game] = games sortBy score

  private def score(game: Game): Float = heuristics map {
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

  private def eloHeuristic(color: Color): Heuristic = game ⇒
    eloBox(game.player(color).elo | 1100)

  private def speedHeuristic: Heuristic = game ⇒
    1 - timeBox(game.estimateTotalTime)

  private def progressHeuristic: Heuristic = game ⇒
    1 - turnBox(game.turns)

  // boxes and reduce to 0..1 range
  private def box(in: Range.Inclusive)(v: Float): Float =
    (math.max(in.start, math.min(v, in.end)) - in.start) / (in.end - in.start).toFloat
}
