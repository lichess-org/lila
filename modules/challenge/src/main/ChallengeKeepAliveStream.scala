package lila.challenge

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus

final class ChallengeKeepAliveStream(api: ChallengeApi)(using
    ec: Executor,
    scheduler: Scheduler
):
  def apply(challenge: Challenge, initialJson: JsObject): Source[JsValue, ?] =
    Source(List(initialJson)).concat(
      Source
        .queue[JsObject](1, akka.stream.OverflowStrategy.dropHead)
        .mapMaterializedValue: queue =>
          val keepAliveInterval = scheduler.scheduleWithFixedDelay(15.seconds, 15.seconds): () =>
            api.ping(challenge.id)
          def completeWith(msg: String) =
            for _ <- queue.offer(Json.obj("done" -> msg)) yield queue.complete()
          val sub = Bus.subscribeFun("challenge"):
            case lila.core.challenge.Event.Accept(c, _) if c.id == challenge.id => completeWith("accepted")
            case Event.Cancel(c) if c.id == challenge.id                        => completeWith("canceled")
            case Event.Decline(c) if c.id == challenge.id                       => completeWith("declined")

          queue
            .watchCompletion()
            .addEffectAnyway:
              keepAliveInterval.cancel()
              Bus.unsubscribe(sub, "challenge")
    )
