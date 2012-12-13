package lila
package team

import scala.collection.mutable

final class Cached(
  teamRepo: TeamRepo,
  memberRepo: MemberRepo) {

  def name(id: String): Option[String] = NameCache(id)

  def teamIds(userId: String): List[String] = TeamIdsCache(userId.pp).pp

  def invalidateTeamIds(userId: String) { TeamIdsCache invalidate userId }

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
