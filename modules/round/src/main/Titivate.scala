package lila.round

import scala.concurrent.duration._

import akka.actor.{ ActorRef, Scheduler }
import play.api.libs.iteratee._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.common.PimpedJson._
import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Query, Game, GameRepo }
import lila.hub.actorApi.map.Tell
import lila.round.actorApi.round.{ Outoftime, Abandon }

private[round] final class Titivate(
    roundMap: ActorRef,
    meddler: Meddler,
    scheduler: Scheduler) {

  def finishByClock: Funit =
    $primitive(Query.candidatesToAutofinish, "_id")(_.asOpt[String]) addEffect { ids ⇒
      println("[titivate] Finish %d games by clock" format ids.size)
      delayBatch(ids, 300.millis) { id ⇒ roundMap ! Tell(id, Outoftime).pp }
    } void

  def finishAbandoned: Funit =
    $primitive(Query.abandoned, "_id", max = 5000.some)(_.asOpt[String]) addEffect { ids ⇒
      println("[titivate] Finish %d abandoned games" format ids.size)
      delayBatch(ids, 200.millis) { id ⇒ roundMap ! Tell(id, Abandon).pp }
    } void

  private def delayBatch[A](batch: Seq[A], duration: FiniteDuration)(op: A ⇒ Unit) {
    batch.zipWithIndex foreach {
      case (a, i) ⇒ scheduler.scheduleOnce((1 + i) * duration) { op(a) }
    }
  }
}
