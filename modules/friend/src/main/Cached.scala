package lila.friend

import lila.memo.AsyncCache

private[friend] final class Cached {

  // UserId => List[UserId] friends of that user
  val friendIds = AsyncCache(FriendRepo.friendUserIds, maxCapacity = 5000)

  // UserId => List[UserId] wanted friends of that user
  val requestedIds = AsyncCache(RequestRepo.requestedUserIds, maxCapacity = 5000)

  // UserId => List[UserId] wanabe friends of that user
  val requesterIds = AsyncCache(RequestRepo.requesterUserIds, maxCapacity = 5000)

  private[friend] def invalidate(userId: ID): Funit = 
      friendIds.remove(userId) >>
        requestedIds.remove(userId) >>
        requesterIds.remove(userId)
}
