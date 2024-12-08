package lila.recap

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }

final class RecapApi(
    colls: RecapColls,
    repo: RecapRepo,
    queue: lila.memo.ParallelMongoQueue[UserId]
)(using Executor):

  export repo.get

  def make(userId: UserId): Fu[Either[Recap.QueueEntry, Recap]] =
    get(userId).flatMap:
      case Some(recap) => fuccess(Right(recap))
      case None        => queue.enqueue(userId).map(_.asLeft)

  def availability(userId: UserId): Fu[Recap.Availability] =
    import Recap.Availability.*
    get(userId).flatMap:
      case Some(recap) => fuccess(Available(recap))
      case None        => queue.enqueue(userId).map(Queued.apply)
