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
    addRequest: () => Unit,
    api: RoundApi,
    nbActors: Int) {

  private object implementation {

    private implicit val timeout = makeTimeout seconds 20

    sealed trait Msg
    case class Player(pov: Pov, apiVersion: Int, ctx: Context) extends Msg
    case class Watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
      analysis: Option[(Pgn, Analysis)] = None,
      initialFenO: Option[Option[String]] = None,
      withMoveTimes: Boolean = false,
      ctx: Context) extends Msg
    case class UserAnalysis(pov: Pov, pref: Pref, initialFen: Option[String]) extends Msg

    private val router = system.actorOf(
      akka.routing.RoundRobinPool(nbActors).props(Props(new lila.hub.SequentialProvider {
        override def debug = true
        def process = {
          case Player(pov, apiVersion, ctx) =>
            api.player(pov, apiVersion)(ctx)
          case Watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, ctx) =>
            api.watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes)(ctx)
          case UserAnalysis(pov, pref, initialFen) =>
            api.userAnalysisJson(pov, pref, initialFen)
        }
      })), "api.round.router")

    private val jsObjectManifest = manifest[JsObject]

    def askRouter(msg: Msg): Fu[JsObject] = {
      addRequest()
      router ? msg mapTo jsObjectManifest
    }
  }

  import implementation._

  def player(pov: Pov, apiVersion: Int)(implicit ctx: Context): Fu[JsObject] =
    askRouter(Player(pov, apiVersion, ctx))

  def watcher(pov: Pov, apiVersion: Int, tv: Option[Boolean],
    analysis: Option[(Pgn, Analysis)] = None,
    initialFenO: Option[Option[String]] = None,
    withMoveTimes: Boolean = false)(implicit ctx: Context): Fu[JsObject] =
    askRouter(Watcher(pov, apiVersion, tv, analysis, initialFenO, withMoveTimes, ctx))

  def userAnalysisJson(pov: Pov, pref: Pref, initialFen: Option[String]): Fu[JsObject] =
    askRouter(UserAnalysis(pov, pref, initialFen))
}
