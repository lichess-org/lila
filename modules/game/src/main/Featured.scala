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
import tube.gameTube

final class Featured(
    lobbySocket: lila.hub.ActorLazyRef,
    rendererActor: lila.hub.ActorLazyRef,
    system: ActorSystem) {

  def one: Future[Option[Game]] = {
    implicit def timeout = makeTimeout(2 seconds)
    (actor ? GetOne mapTo manifest[Option[Game]]) nevermind "[featured] one"
  }

  private val actor = system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {
      // this message must block to make the actor wait for the result
      // before accepting new messages
      case GetOne ⇒ sender ! getOne.nevermind("[featured] GetOne").await
    }

    private def getOne: Fu[Option[Game]] = {
      implicit def timeout = makeTimeout(2 seconds)
      oneId ?? $find.byId[Game] map (_ filter valid) flatMap {
        _.fold({
          feature addEffect { newOne ⇒
            oneId = newOne map (_.id)
            newOne foreach { game ⇒
              rendererActor ? actorApi.RenderFeaturedJs(game) onSuccess {
                case html: Html ⇒ lobbySocket ! actorApi.ChangeFeatured(html)
              }
            }
          }
        })(game ⇒ fuccess(game.some))
      }
    }

    private def valid(game: Game) = game.isBeingPlayed

    private def feature: Fu[Option[Game]] = GameRepo.featuredCandidates map { games ⇒
      Featured.sort(games filter valid).headOption
    }
  }))
}

object Featured {

  private case object GetOne

  def sort(games: List[Game]): List[Game] = games sortBy { -score(_) }

  private def score(game: Game): Float = heuristics map {
    case (fn, coefficient) ⇒ heuristicBox(fn(game)) * coefficient
  } sum

  private type Heuristic = Game ⇒ Float
  private val heuristicBox = box(0 to 1) _
  private val eloBox = box(1000 to 2000) _
  private val timeBox = box(60 to 300) _
  private val turnBox = box(1 to 21) _

  private val heuristics: List[(Heuristic, Float)] = List(
    eloHeuristic(Color.White) -> 1.5f,
    eloHeuristic(Color.Black) -> 1.5f,
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
