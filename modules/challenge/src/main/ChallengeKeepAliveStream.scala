package lila.challenge

import lila.core.challenge.PositiveEvent
import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus

final class ChallengeKeepAliveStream(api: ChallengeApi)(using
    ec: Executor,
    scheduler: Scheduler
):
  def apply(challenge: Challenge, initialJson: JsObject)(
      createTheChallengeNow: () => Funit
  ): Source[JsValue, ?] =
    Source(List(initialJson)).concat:
      Source
        .queue[JsObject](1, akka.stream.OverflowStrategy.dropHead)
        .mapMaterializedValue: queue =>

          val keepAliveInterval = scheduler.scheduleWithFixedDelay(15.seconds, 15.seconds): () =>
            api.ping(challenge.id)

          def completeWith(msg: String) =
            for _ <- queue.offer(Json.obj("done" -> msg))
            yield queue.complete()

          val subPositive = Bus.sub[PositiveEvent]:
            case PositiveEvent.Accept(c, _) if c.id == challenge.id => completeWith("accepted")

          val subNegative = Bus.sub[NegativeEvent]:
            case NegativeEvent.Decline(c) if c.id == challenge.id => completeWith("declined")
            case NegativeEvent.Cancel(c) if c.id == challenge.id  => completeWith("canceled")

          for
            _ <- createTheChallengeNow()
            q <- queue
              .watchCompletion()
              .addEffectAnyway:
                keepAliveInterval.cancel()
                Bus.unsub[PositiveEvent](subPositive)
                Bus.unsub[NegativeEvent](subNegative)
          yield q
