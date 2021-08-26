package lila.challenge

import akka.stream.scaladsl._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.Bus

final class ChallengeKeepAliveStream(api: ChallengeApi)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) {
  def apply(challenge: Challenge, initialJson: JsObject): Source[JsValue, _] =
    Source(List(initialJson)) concat
      Source.queue[JsObject](1, akka.stream.OverflowStrategy.dropHead).mapMaterializedValue { queue =>
        val keepAliveInterval = scheduler.scheduleWithFixedDelay(15 seconds, 15 seconds) { () =>
          api.ping(challenge.id).unit
        }
        def completeWith(msg: String) = {
          queue.offer(Json.obj("done" -> msg)) >>- queue.complete()
        }.unit
        val sub = Bus.subscribeFun("challenge") {
          case Event.Accept(c, _) if c.id == challenge.id => completeWith("accepted")
          case Event.Cancel(c) if c.id == challenge.id    => completeWith("canceled")
          case Event.Decline(c) if c.id == challenge.id   => completeWith("declined")
        }
        queue.watchCompletion().foreach { _ =>
          keepAliveInterval.cancel()
          Bus.unsubscribe(sub, "challenge")
        }
      }
}
