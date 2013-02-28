package lila.app
package team

import user.User

import scala.collection.mutable

final class Cached(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo) {

  def name(id: String): Option[String] = NameCache(id)

  def teamIds(userId: String): List[String] = TeamIdsCache(userId)
  def teamIds(user: User): List[String] = teamIds(user.id)
  def invalidateTeamIds(userId: String) { TeamIdsCache invalidate userId }

  def nbRequests(userId: String): Int = NbRequestsCache(userId)
  def nbRequests(user: User): Int = nbRequests(user.id)
  def invalidateNbRequests(userId: String) { NbRequestsCache invalidate userId }

  private object NbRequestsCache {

    def apply(userId: String): Int = cache.getOrElseUpdate(userId,
      (teamRepo teamIdsByCreator userId flatMap requestRepo.countByTeamIds).unsafePerformIO
    )

    def invalidate(userId: String) { cache -= userId }

    // userId => number
    private val cache = mutable.Map[String, Int]()
  }

  private object NameCache {

    def apply(id: String): Option[String] = cache.getOrElseUpdate(id,
      (teamRepo name id).unsafePerformIO
    )

    // id => name
    private val cache = mutable.Map[String, Option[String]]()
  }

  private object TeamIdsCache {

    def apply(userId: String): List[String] = cache.getOrElseUpdate(userId,
      (memberRepo teamIdsByUserId userId).unsafePerformIO
    )

    def invalidate(userId: String) { cache -= userId }

    // id => name
    private val cache = mutable.Map[String, List[String]]()
  }
}
