package lila.recap

import play.api.libs.json.*

import lila.common.LilaFuture.delay
import lila.core.lilaism.LilaException
import lila.recap.Recap.Availability

final class RecapApi(
    repo: RecapRepo,
    recapJson: RecapJson,
    queue: lila.memo.ParallelMongoQueue[UserId]
)(using Executor, Scheduler):

  export repo.get

  def availability[C: Writes](user: User)(getCosts: => Fu[C]): Fu[Availability] =
    import Availability.*
    get(user.id)
      .flatMap:
        case Some(recap) => recapJson(recap, user).map(Available(_))
        case None => for _ <- queue.enqueue(user.id) yield Queued(recapJson(user))
      .flatMap(addCosts(getCosts))

  def addCosts[C: Writes](getCosts: => Fu[C])(av: Availability): Fu[Availability] = av match
    case Availability.Available(data) =>
      getCosts.map: costs =>
        Availability.Available(data.add("costs" -> costs.some))
    case av => fuccess(av)

  // waits until the recap is computed
  def awaiter[C: Writes](user: User, counter: Int = 0)(getCosts: => Fu[C]): Fu[JsObject] =
    availability(user)(getCosts).flatMap:
      case Recap.Availability.Available(data) => fuccess(data)
      case Recap.Availability.Queued(_) =>
        if counter < 100
        then delay(1.second)(awaiter(user, counter + 1)(getCosts))
        else fufail(LilaException(s"Recap awaiter timeout for ${user.id}"))
