package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.hub.actorApi.ai.GetLoad
import makeTimeout.short

private[ai] final class Monitor(queue: ActorRef) extends Actor {

  private var times = Map[Long, Int]()

  private val loadPeriodMillis = 5000

  def receive = {

    case AddTime(time) => times = times + (nowMillis -> time)

    case GetLoad => {
      val now = nowMillis
      val from = nowMillis - loadPeriodMillis
      val time = times.filter(_._1 >= from).map(_._2).sum
      sender ! ((time * 100) / loadPeriodMillis)
      times = times filterNot (_._1 < from)
    }
  }
}
