package lila.game

import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.Color
import Featured._
import play.api.Play.current
import play.api.templates.Html

import lila.db.api._
import lila.hub.actorApi.map.TellAll
import tube.gameTube

final class Featured(
    lobbySocket: lila.hub.ActorLazyRef,
    roundSocket: lila.hub.ActorLazyRef,
    rendererActor: lila.hub.ActorLazyRef,
    system: ActorSystem) {

  implicit private def timeout = makeTimeout(2 seconds)

  def one: Future[Option[Game]] = {
    (actor ? Get mapTo manifest[Option[Game]]) nevermind "[featured] one"
  }

  private val actor = system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {

      case Get ⇒ oneId ?? $find.byId[Game] pipeTo sender

      case Set(game) ⇒ {
        oneId = game.id.some
        roundSocket ! actorApi.ChangeFeaturedId(game.id)
        rendererActor ? actorApi.RenderFeaturedJs(game) onSuccess {
          case html: Html ⇒ lobbySocket ! actorApi.ChangeFeatured(html)
        }
      }

      case Update ⇒ {
        oneId ?? $find.byId[Game] foreach {
          case None                      ⇒ feature foreach elect
          case Some(game) if fresh(game) ⇒
          case Some(game)                ⇒ featureFrom(game) foreach elect
        }
      }
    }

    def elect(gameOption: Option[Game]) {
      gameOption foreach { self ! Set(_) }
    }

    def fresh(game: Game) = game.isBeingPlayed

    type Fuog = Fu[Option[Game]]

    def featureFrom(game: Game): Fuog =
      wayBetter(game) orElse rematch(game) orElse featureOld(game)

    def wayBetter(game: Game): Fuog = feature map {
      case Some(next) if isWayBetter(game.copy(turns = 1), next) ⇒ next.some
      case _ ⇒ none
    }

    def isWayBetter(g1: Game, g2: Game) = score(g2) > (score(g1) * 1.5)

    def rematch(game: Game): Fuog = game.next ?? $find.byId[Game]

    def featureOld(game: Game): Fuog = (game olderThan 6) ?? feature

    def feature: Fu[Option[Game]] = GameRepo.featuredCandidates map { games ⇒
      Featured.sort(games filter fresh).headOption
    } flatMap {
      case None       ⇒ GameRepo random 1 map (_.headOption)
      case Some(game) ⇒ fuccess(game.some)
    }
  }))

  system.scheduler.schedule(0 seconds, 1 seconds, actor, Update)
}

object Featured {

  private case object Get
  private case object Update
  private case class Set(game: Game)

  def sort(games: List[Game]): List[Game] = games sortBy { -score(_) }

  private def score(game: Game): Int = math.round {
    (heuristics map {
      case (fn, coefficient) ⇒ heuristicBox(fn(game)) * coefficient
    }).sum * 1000
  }

  private type Heuristic = Game ⇒ Float
  private val heuristicBox = box(0 to 1) _
  private val eloBox = box(1000 to 2000) _
  private val timeBox = box(60 to 360) _
  private val turnBox = box(1 to 25) _

  private val heuristics: List[(Heuristic, Float)] = List(
    eloHeuristic(Color.White) -> 1f,
    eloHeuristic(Color.Black) -> 1f,
    speedHeuristic -> 1f,
    progressHeuristic -> 1f)

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
