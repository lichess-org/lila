package lila.api

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import lila.common.ApiVersion
import lila.analyse.Analysis
import lila.game.Pov
import lila.pref.Pref

private[api] final class RoundApiBalancer(
    system: ActorSystem,
    api: RoundApi,
    nbActors: Int) {

  private val logger = lila.log("round").branch("balancer")

  private object implementation {

    implicit val timeout = makeTimeout seconds 20

    case class Player(pov: Pov, apiVersion: ApiVersion, ctx: Context)
    case class Watcher(pov: Pov, apiVersion: ApiVersion, tv: Option[lila.round.OnTv],
      initialFenO: Option[Option[String]] = None,
      ctx: Context)
    case class Review(pov: Pov, apiVersion: ApiVersion, tv: Option[lila.round.OnTv],
      analysis: Option[Analysis] = None,
      initialFenO: Option[Option[String]] = None,
      withMoveTimes: Boolean = false,
      withDivision: Boolean = false,
      withOpening: Boolean = false,
      ctx: Context)
    case class UserAnalysis(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color, owner: Boolean)
    case class FreeStudy(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color)

    val router = system.actorOf(
      akka.routing.RoundRobinPool(nbActors).props(Props(new lila.hub.SequentialProvider {
        val futureTimeout = 20.seconds
        val logger = RoundApiBalancer.this.logger
        def process = {
          case Player(pov, apiVersion, ctx) => {
            api.player(pov, apiVersion)(ctx) addFailureEffect { e =>
              logger.error(pov.toString, e)
            }
          }.chronometer.logIfSlow(500, logger) { _ => s"inner player $pov" }.result
          case Watcher(pov, apiVersion, tv, initialFenO, ctx) =>
            api.watcher(pov, apiVersion, tv, initialFenO)(ctx)
          case Review(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, withDivision, withOpening, ctx) =>
            api.review(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, withDivision, withOpening)(ctx)
          case UserAnalysis(pov, pref, initialFen, orientation, owner) =>
            api.userAnalysisJson(pov, pref, initialFen, orientation, owner)
          case FreeStudy(pov, pref, initialFen, orientation) =>
            api.freeStudyJson(pov, pref, initialFen, orientation)
        }
      })), "api.round.router")
  }

  import implementation._

  def player(pov: Pov, apiVersion: ApiVersion)(implicit ctx: Context): Fu[JsObject] = {
    router ? Player(pov, apiVersion, ctx) mapTo manifest[JsObject] addFailureEffect { e =>
      logger.error(pov.toString, e)
    }
  }.chronometer
    .mon(_.round.api.player)
    .logIfSlow(500, logger) { _ => s"outer player $pov" }
    .result

  def watcher(pov: Pov, apiVersion: ApiVersion, tv: Option[lila.round.OnTv],
    initialFenO: Option[Option[String]] = None)(implicit ctx: Context): Fu[JsObject] = {
    router ? Watcher(pov, apiVersion, tv, initialFenO, ctx) mapTo manifest[JsObject]
  }.mon(_.round.api.watcher)

  def review(pov: Pov, apiVersion: ApiVersion, tv: Option[lila.round.OnTv],
    analysis: Option[Analysis] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false,
    withDivision: Boolean = false,
    withOpening: Boolean = false)(implicit ctx: Context): Fu[JsObject] = {
    router ? Review(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, withDivision, withOpening, ctx) mapTo manifest[JsObject]
  }.mon(_.round.api.watcher)

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color, owner: Boolean): Fu[JsObject] =
    router ? UserAnalysis(pov, pref, initialFen, orientation, owner) mapTo manifest[JsObject]

  def freeStudyJson(pov: Pov, pref: Pref, initialFen: Option[String], orientation: chess.Color): Fu[JsObject] =
    router ? FreeStudy(pov, pref, initialFen, orientation) mapTo manifest[JsObject]
}
