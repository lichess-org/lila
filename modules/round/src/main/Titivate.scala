package lila.round

import scala.concurrent.duration._

import play.api.libs.iteratee._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.common.PimpedJson._
import lila.common.Scheduler
import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Query, Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.{ Outoftime, Abandon }

private[round] final class Titivate(
    roundMap: akka.actor.ActorRef,
    meddler: Meddler,
    scheduler: Scheduler) {

  def finishByClock: Funit =
    $primitive(Query.finishByClock, "_id", max = 5000.some)(_.asOpt[String]) addEffect { ids =>
      loginfo("[titivate] Finish %d games by clock" format ids.size)
      scheduler.throttle(100.millis)(ids) { id => roundMap ! Tell(id, Outoftime) }
    } void

  def finishAbandoned: Funit =
    $primitive(Query.abandoned, "_id", max = 5000.some)(_.asOpt[String]) addEffect { ids =>
      loginfo("[titivate] Finish %d abandoned games" format ids.size)
      scheduler.throttle(100.millis)(ids) { id => roundMap ! Tell(id, Abandon) }
    } void
}
