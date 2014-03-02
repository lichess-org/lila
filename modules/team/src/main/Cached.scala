package lila.team

import lila.memo.AsyncCache

private[team] final class Cached(capacity: Int) {

  val name = AsyncCache(TeamRepo.name, maxCapacity = capacity)

  val teamIds = AsyncCache(MemberRepo.teamIdsByUser, maxCapacity = capacity)

  val nbRequests = AsyncCache(
    (userId: String) => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    maxCapacity = capacity)
}
