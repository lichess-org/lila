package lila.recap

import play.api.libs.json.*

import lila.common.LilaFuture.delay
import lila.core.lilaism.LilaException
import lila.recap.Recap.Availability

final class RecapApi(
    repo: RecapRepo,
    recapJson: RecapJson,
    queue: ParallelMongoQueue[UserId]
)(using Executor, Scheduler):

  export repo.get

  def availability(user: User): Fu[Availability] =
    import Availability.*
    get(user.id).flatMap:
      _.fold(for _ <- queue.enqueue(user.id) yield Queued(recapJson(user))): recap =>
        recapJson(recap, user).map(Available(_))

  // waits until the recap is computed
  def awaiter(user: User, counter: Int = 0): Fu[JsObject] =
    availability(user).flatMap:
      case Recap.Availability.Available(data) => fuccess(data)
      case Recap.Availability.Queued(_) =>
        if counter < 60
        then delay(2.seconds)(awaiter(user, counter + 1))
        else fufail(LilaException(s"Recap awaiter timeout for ${user.id}"))
