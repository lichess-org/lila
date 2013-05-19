package lila.friend

import lila.memo.AsyncCache

private[friend] final class Cached {

  val friendIds = AsyncCache(FriendRepo.friendUserIds, maxCapacity = 5000)

  val nbRequests = AsyncCache(RequestRepo.countByFriendId, maxCapacity = 5000)
}
