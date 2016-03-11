package lila.api

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import chess.format.pgn.Pgn
import lila.analyse.Analysis
import lila.game.Pov
import lila.pref.Pref

private[api] final class RoundApiBalancer(
    system: ActorSystem,
    api: RoundApi,
    nbActors: Int) {

  private object implementation {

    implicit val timeout = makeTimeout seconds 20

    case class Player(pov: Pov, apiVersion: Int, ctx: Context)
    case class Watcher(pov: Pov, apiVersion: Int, tv: Option[lila.round.OnTv],
      analysis: Option[(Pgn, Analysis)] = None,
      initialFenO: Option[Option[String]] = None,
      withMoveTimes: Boolean = false,
      withOpening: Boolean = false,
      ctx: Context)
    case class UserAnalysis(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color, owner: Boolean)

    val router = system.actorOf(
      akka.routing.RoundRobinPool(nbActors).props(Props(new lila.hub.SequentialProvider {
        val futureTimeout = 20.seconds
        def process = {
          case Player(pov, apiVersion, ctx) => {
            api.player(pov, apiVersion)(ctx) addFailureEffect { e =>
              play.api.Logger("RoundApiBalancer").error(s"$pov $e")
            }
          }.chronometer.logIfSlow(500, "RoundApiBalancer") { _ => s"inner player $pov" }.result
          case Watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, withOpening, ctx) =>
            api.watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, withOpening)(ctx)
          case UserAnalysis(pov, pref, initialFen, orientation, owner) =>
            api.userAnalysisJson(pov, pref, initialFen, orientation, owner)
        }
      })), "api.round.router")
  }

  import implementation._

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] = {
    router ? Player(pov, apiVersion, ctx) mapTo manifest[JsObject] addFailureEffect { e =>
      play.api.Logger("RoundApiBalancer").error(s"$pov $e")
    }
  }.chronometer
    .mon(_.round.api.player)
    .logIfSlow(500, "RoundApiBalancer") { _ => s"outer player $pov" }
    .result

  def watcher(pov: Pov, apiVersion: Int, tv: Option[lila.round.OnTv],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false,
    withOpening: Boolean = false)(implicit ctx: Context): Fu[JsObject] = {
    router ? Watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, withOpening, ctx) mapTo manifest[JsObject]
  }.mon(_.round.api.watcher)

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color, owner: Boolean): Fu[JsObject] =
    router ? UserAnalysis(pov, pref, initialFen, orientation, owner) mapTo manifest[JsObject]
}
