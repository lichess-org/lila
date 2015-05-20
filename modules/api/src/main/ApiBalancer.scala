package lila.api

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.JsObject

import chess.format.pgn.Pgn
import lila.analyse.Analysis
import lila.game.Pov
import lila.pref.Pref

private[api] final class RoundApiBalancer(
    system: ActorSystem,
    api: RoundApi,
    nbActors: Int) {

  private object implementation {

    implicit val timeout = makeTimeout seconds 30

    case class Player(pov: Pov, apiVersion: Int, ctx: Context)
    case class Watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
      analysis: Option[(Pgn, Analysis)] = None,
      initialFenO: Option[Option[String]] = None,
      withMoveTimes: Boolean = false,
      ctx: Context)
    case class UserAnalysis(pov: Pov, pref: Pref, initialFen: Option[String])

    val router = system.actorOf(
      akka.routing.RoundRobinPool(nbActors).props(Props(new lila.hub.SequentialProvider {
        def process = {
          case Player(pov, apiVersion, ctx) =>
            api.player(pov, apiVersion)(ctx)
          case Watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, ctx) =>
            api.watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes)(ctx)
          case UserAnalysis(pov, pref, initialFen) =>
            api.userAnalysisJson(pov, pref, initialFen)
        }
      })), "api.round.router")
  }

  import implementation._

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    router ? Player(pov, apiVersion, ctx) mapTo manifest[JsObject]

  def watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false)(implicit ctx: Context): Fu[JsObject] =
    router ? Watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, ctx) mapTo manifest[JsObject]

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String]): Fu[JsObject] =
    router ? UserAnalysis(pov, pref, initialFen) mapTo manifest[JsObject]
}
