package lila.recap

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.recap.RecapJson.given
import lila.common.LilaFuture.delay
import play.api.libs.json.JsObject
import lila.core.lilaism.LilaException

final class RecapApi(
    colls: RecapColls,
    repo: RecapRepo,
    queue: lila.memo.ParallelMongoQueue[UserId]
)(using Executor, Scheduler):

  export repo.get

  def availability(user: User): Fu[Recap.Availability] =
    import Recap.Availability.*
    get(user.id).flatMap:
      case Some(recap) => fuccess(Available(RecapJson(recap, user)))
      case None        => queue.enqueue(user.id).map(Queued.apply)

  // waits until the recap is computed
  def awaiter(user: User, counter: Int = 0): Fu[JsObject] =
    availability(user).flatMap:
      case Recap.Availability.Available(data) => fuccess(data)
      case Recap.Availability.Queued(_) =>
        if counter < 100
        then delay(1.second)(awaiter(user, counter + 1))
        else fufail(LilaException(s"Recap awaiter timeout for ${user.id}"))
