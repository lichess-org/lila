package lila.tv

import scala.concurrent.duration._

import akka.actor._
import akka.actor.ActorSelection
import akka.pattern.{ ask, pipe }
import chess.Color
import Featured._
import play.api.templates.Html

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo }

final class Featured(
    lobbySocket: ActorSelection,
    rendererActor: ActorSelection,
    system: ActorSystem) {

  implicit private def timeout = makeTimeout(50 millis)

  private type Fuog = Fu[Option[Game]]

  private val bus = system.lilaBus

  def one: Fuog =
    (actor ? Get mapTo manifest[Option[String]]) recover {
      case _: Exception => none
    } flatMap { _ ?? GameRepo.game }

  private[tv] val actor = system.actorOf(Props(new Actor {

    private var oneId = none[String]

    def receive = {

      case Get => sender ! oneId

      case Set(game) =>
        oneId = game.id.some
        rendererActor ? actorApi.RenderFeaturedJs(game) onSuccess {
          case html: Html =>
            bus.publish(lila.hub.actorApi.game.ChangeFeatured(game.id, html), 'changeFeaturedGame)
        }
        GameRepo setTv game.id

      case Continue =>
        oneId ?? $find.byId[Game] foreach {
          case None                       => feature foreach elect
          case Some(game) if !fresh(game) => wayBetter(game) orElse rematch(game) orElse featureIfOld(game) foreach elect
          case _                          =>
        }

      case Disrupt =>
        oneId ?? $find.byId[Game] foreach {
          case Some(game) if fresh(game) => wayBetter(game) foreach elect
          case _                         =>
        }
    }

    def elect(gameOption: Option[Game]) {
      gameOption foreach { self ! Set(_) }
    }

    def fresh(game: Game) = game.isBeingPlayed

    def wayBetter(game: Game): Fuog = feature map {
      case Some(next) if isWayBetter(game, next) => next.some
      case _                                     => none
    }

    def isWayBetter(g1: Game, g2: Game) =
      score(g2.resetTurns) > (score(g1.resetTurns) * 1.1)

    def rematch(game: Game): Fuog = game.next ?? $find.byId[Game]

    def featureIfOld(game: Game): Fuog = (game olderThan 7) ?? feature

    def feature: Fuog = GameRepo.featuredCandidates map { games =>
      Featured.sort(games filter fresh).headOption
    } orElse GameRepo.random
  }))

  actor ! Continue
}

object Featured {

  private case object Get
  private case class Set(game: Game)
  case object Continue
  case object Disrupt

  def sort(games: List[Game]): List[Game] = games sortBy { -score(_) }

  private[tv] def score(game: Game): Int = math.round {
    (heuristics map {
      case (fn, coefficient) => heuristicBox(fn(game)) * coefficient
    }).sum * 1000
  }

  private type Heuristic = Game => Float
  private val heuristicBox = box(0 to 1) _
  private val ratingBox = box(1000 to 2600) _
  private val timeBox = box(60 to 360) _
  private val turnBox = box(1 to 25) _

  private val heuristics: List[(Heuristic, Float)] = List(
    ratingHeuristic(Color.White) -> 1.2f,
    ratingHeuristic(Color.Black) -> 1.2f,
    speedHeuristic -> 0.5f,
    progressHeuristic -> 0.7f)

  private[tv] def ratingHeuristic(color: Color): Heuristic = game =>
    ratingBox(game.player(color).rating | 1100)

  private[tv] def speedHeuristic: Heuristic = game =>
    1 - timeBox(game.estimateTotalTime)

  private[tv] def progressHeuristic: Heuristic = game =>
    1 - turnBox(game.turns)

  // boxes and reduces to 0..1 range
  private[tv] def box(in: Range.Inclusive)(v: Float): Float =
    (math.max(in.start, math.min(v, in.end)) - in.start) / (in.end - in.start).toFloat
}
