package lila.game

import scala.concurrent.duration._

import play.api.libs.json._

import lila.common.Scheduler
import lila.db.api._
import tube.gameTube

private[game] final class Titivate(
    remover: Remover,
    scheduler: Scheduler) {

  def cleanupUnplayed: Funit =
    $primitive(Query.unplayed, "_id", max = 10000.some)(_.asOpt[String]) addEffect { ids ⇒
      println("[titivate] Remove %d unplayed games" format ids.size)
      scheduler.throttle(1.second)(ids grouped 100 toSeq)(remover.apply)
    } void
}
