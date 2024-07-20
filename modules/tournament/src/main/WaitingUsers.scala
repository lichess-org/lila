package lila.tournament

import chess.Clock.Config as TournamentClock
import scalalib.cache.ExpireSetMemo

private case class WaitingUsers(
    hash: Map[UserId, Instant],
    apiUsers: Option[ExpireSetMemo[UserId]],
    clock: TournamentClock,
    date: Instant
)(using Executor):

  // ultrabullet -> 8
  // hyperbullet -> 10
  // 1+0  -> 12  -> 15
  // 3+0  -> 24  -> 24
  // 5+0  -> 36  -> 36
  // 10+0 -> 66  -> 50
  private val waitSeconds: Int =
    if clock.estimateTotalSeconds < 30 then 8
    else if clock.estimateTotalSeconds < 60 then 10
    else (clock.estimateTotalSeconds / 10 + 6).atMost(50).atLeast(15)

  lazy val all  = hash.keySet
  lazy val size = hash.size

  def isOdd = size % 2 == 1

  // skips the most recent user if odd
  def evenNumber: Set[UserId] =
    if isOdd then all - hash.maxBy(_._2.toMillis)._1
    else all

  lazy val haveWaitedEnough: Boolean =
    size > 100 || {
      val since                      = date.minusSeconds(waitSeconds)
      val nbConnectedLongEnoughUsers = hash.count { case (_, d) => d.isBefore(since) }
      nbConnectedLongEnoughUsers > 1
    }

  def update(fromWebSocket: Set[UserId]) =
    val newDate      = nowInstant
    val withApiUsers = fromWebSocket ++ apiUsers.so(_.keySet)
    copy(
      date = newDate,
      hash = {
        hash.view.filterKeys(withApiUsers.contains) ++
          withApiUsers.filterNot(hash.contains).map { _ -> newDate }
      }.toMap
    )

  def hasUser(userId: UserId) = hash contains userId

  def addApiUser(userId: UserId) =
    val memo = apiUsers | new ExpireSetMemo[UserId](70 seconds)
    memo.put(userId)
    if apiUsers.isEmpty then copy(apiUsers = memo.some) else this

  def removePairedUsers(us: Set[UserId]) =
    apiUsers.foreach(_.removeAll(us))
    copy(hash = hash -- us)

final private class WaitingUsersApi(using Executor):

  private val store = new java.util.concurrent.ConcurrentHashMap[TourId, WaitingUsers.WithNext](64)

  def hasUser(tourId: TourId, userId: UserId): Boolean =
    Option(store.get(tourId)).exists(_.waiting.hasUser(userId))

  def registerNextPromise(tour: Tournament, promise: Promise[WaitingUsers]) =
    updateOrCreate(tour)(_.copy(next = promise.some))

  def registerWaitingUsers(tourId: TourId, users: Set[UserId]) =
    store.computeIfPresent(
      tourId,
      (_: TourId, cur: WaitingUsers.WithNext) =>
        val newWaiting = cur.waiting.update(users)
        cur.next.foreach(_.success(newWaiting))
        WaitingUsers.WithNext(newWaiting, none)
    )

  def registerPairedUsers(tourId: TourId, users: Set[UserId]) =
    store.computeIfPresent(
      tourId,
      (_: TourId, cur: WaitingUsers.WithNext) => cur.copy(waiting = cur.waiting.removePairedUsers(users))
    )

  def addApiUser(tour: Tournament, user: User) = updateOrCreate(tour) { w =>
    w.copy(waiting = w.waiting.addApiUser(user.id))
  }

  def remove(id: TourId) = store.remove(id)

  private def updateOrCreate(tour: Tournament)(f: WaitingUsers.WithNext => WaitingUsers.WithNext) =
    store.compute(
      tour.id,
      (_: TourId, cur: WaitingUsers.WithNext) =>
        f(
          Option(cur) | WaitingUsers.WithNext(
            WaitingUsers(Map.empty, None, tour.clock, nowInstant),
            none
          )
        )
    )

private object WaitingUsers:
  case class WithNext(waiting: WaitingUsers, next: Option[Promise[WaitingUsers]])
