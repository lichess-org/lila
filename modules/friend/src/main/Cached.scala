package lila.friend

import lila.memo.AsyncCache

private[friend] final class Cached {

  // UserId => List[UserId] friends of that user
  val friendIds = AsyncCache(FriendRepo.friendUserIds, maxCapacity = 5000)

  // UserId => List[UserId] wanted friends of that user
  val requestIds = AsyncCache(RequestRepo.requestedUserIds, maxCapacity = 5000)

  val nbRequests = AsyncCache(RequestRepo.countByFriendId, maxCapacity = 5000)

  private[friend] def invalidate(userId: ID): Funit = 
      friendIds.remove(userId) >>
        requestIds.remove(userId) >>
        nbRequests.remove(userId)
}
