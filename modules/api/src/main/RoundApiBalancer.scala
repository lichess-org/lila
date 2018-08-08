package lidraughts.api

import akka.actor._
import akka.pattern.ask
import play.api.libs.json.JsObject
import scala.concurrent.duration._

import draughts.format.FEN
import lidraughts.analyse.Analysis
import lidraughts.common.ApiVersion
import lidraughts.game.Pov
import lidraughts.pref.Pref
import lidraughts.round.JsonView.WithFlags
import lidraughts.user.User

private[api] final class RoundApiBalancer(
    system: ActorSystem,
    api: RoundApi,
    nbActors: Int
) {

  private val logger = lidraughts.log("round").branch("balancer")

  private object implementation {

    implicit val timeout = makeTimeout seconds 20

    case class Player(pov: Pov, apiVersion: ApiVersion, ctx: Context)
    case class Watcher(pov: Pov, apiVersion: ApiVersion, tv: Option[lidraughts.round.OnTv],
        initialFenO: Option[Option[FEN]] = None,
        ctx: Context)
    case class Review(pov: Pov, apiVersion: ApiVersion, tv: Option[lidraughts.round.OnTv],
        analysis: Option[Analysis] = None,
        initialFenO: Option[Option[FEN]] = None,
        withFlags: WithFlags,
        ctx: Context)
    case class UserAnalysis(pov: Pov, pref: Pref, initialFen: Option[FEN], orientation: draughts.Color, owner: Boolean, me: Option[User], iteratedCapts: Boolean)
    case class FreeStudy(pov: Pov, pref: Pref, initialFen: Option[FEN], orientation: draughts.Color, me: Option[User])

    val router = system.actorOf(
      akka.routing.RoundRobinPool(nbActors).props(Props(new lidraughts.hub.SequentialProvider {
        val futureTimeout = 20.seconds
        val logger = RoundApiBalancer.this.logger
        def process = {
          case Player(pov, apiVersion, ctx) => {
            logger.info(s"process player pov game: ${pov.game.turnColor}");
            {
              api.player(pov, apiVersion)(ctx) addFailureEffect { e =>
                logger.error(s"player ${pov.toString}", e)
              }
            }.chronometer
              .mon(_.round.api.playerInner)
              .logIfSlow(500, logger) { _ => s"inner player $pov" }
              .result
          }
          case Watcher(pov, apiVersion, tv, initialFenO, ctx) =>
            api.watcher(pov, apiVersion, tv, initialFenO)(ctx)
          case Review(pov, apiVersion, tv, analysis, initialFenO, withFlags, ctx) =>
            api.review(pov, apiVersion, tv, analysis, initialFenO, withFlags)(ctx)
          case UserAnalysis(pov, pref, initialFen, orientation, owner, me, iteratedCapts) =>
            api.userAnalysisJson(pov, pref, initialFen, orientation, owner, me, iteratedCapts)
          case FreeStudy(pov, pref, initialFen, orientation, me) =>
            fuccess(api.freeStudyJson(pov, pref, initialFen, orientation, me))
        }
      })), "api.round.router"
    )
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

  def watcher(pov: Pov, apiVersion: ApiVersion, tv: Option[lidraughts.round.OnTv],
    initialFenO: Option[Option[FEN]] = None)(implicit ctx: Context): Fu[JsObject] = {
    router ? Watcher(pov, apiVersion, tv, initialFenO, ctx) mapTo manifest[JsObject]
  }.mon(_.round.api.watcher)

  def review(pov: Pov, apiVersion: ApiVersion,
    tv: Option[lidraughts.round.OnTv] = None,
    analysis: Option[Analysis] = None,
    initialFenO: Option[Option[FEN]] = None,
    withFlags: WithFlags)(implicit ctx: Context): Fu[JsObject] = {
    router ? Review(pov, apiVersion, tv, analysis, initialFenO, withFlags, ctx) mapTo manifest[JsObject]
  }.mon(_.round.api.watcher)

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[FEN], orientation: draughts.Color, owner: Boolean, me: Option[User], iteratedCapts: Boolean = false): Fu[JsObject] =
    router ? UserAnalysis(pov, pref, initialFen, orientation, owner, me, iteratedCapts) mapTo manifest[JsObject]

  def freeStudyJson(pov: Pov, pref: Pref, initialFen: Option[FEN], orientation: draughts.Color, me: Option[User]): Fu[JsObject] =
    router ? FreeStudy(pov, pref, initialFen, orientation, me) mapTo manifest[JsObject]
}
