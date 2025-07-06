package lila.challenge

import akka.stream.scaladsl.*
import play.api.libs.json.*

final class ChallengeKeepAliveStream(api: ChallengeApi)(using
    ec: Executor,
    scheduler: Scheduler
):
  def apply(
      challenge: Challenge,
      initialJson: JsObject,
      challengeResult: Future[String]
  ): Source[JsValue, ?] =
    Source(List(initialJson)).concat:
      Source
        .queue[JsObject](1, akka.stream.OverflowStrategy.dropHead)
        .mapMaterializedValue: queue =>

          val keepAliveInterval = scheduler.scheduleWithFixedDelay(15.seconds, 15.seconds): () =>
            api.ping(challenge.id)

          def completeWith(msg: String) =
            for _ <- queue.offer(Json.obj("done" -> msg)) yield queue.complete()

          challengeResult.map(completeWith)

          queue
            .watchCompletion()
            .addEffectAnyway:
              keepAliveInterval.cancel()
