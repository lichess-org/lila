package lila.recap

import lila.common.LilaFuture.delay
import play.api.libs.json.JsObject
import lila.core.lilaism.LilaException

final class RecapApi(
    repo: RecapRepo,
    recapJson: RecapJson,
    queue: lila.memo.ParallelMongoQueue[UserId]
)(using Executor, Scheduler):

  export repo.get

  def availability(user: User): Fu[Recap.Availability] =
    import Recap.Availability.*
    get(user.id).flatMap:
      case Some(recap) => recapJson(recap, user).map(Available(_))
      case None => for _ <- queue.enqueue(user.id) yield Queued(recapJson(user))

  // waits until the recap is computed
  def awaiter(user: User, counter: Int = 0): Fu[JsObject] =
    availability(user).flatMap:
      case Recap.Availability.Available(data) => fuccess(data)
      case Recap.Availability.Queued(_) =>
        if counter < 100
        then delay(1.second)(awaiter(user, counter + 1))
        else fufail(LilaException(s"Recap awaiter timeout for ${user.id}"))
