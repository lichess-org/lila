package lila.relation

import lila.memo.AsyncCache

private[relation] final class Cached {

  private[relation] val followers = AsyncCache(RelationRepo.followers, maxCapacity = 5000)
  private[relation] val following = AsyncCache(RelationRepo.following, maxCapacity = 5000)
  private[relation] val blockers = AsyncCache(RelationRepo.blockers, maxCapacity = 5000)
  private[relation] val blocking = AsyncCache(RelationRepo.blocking, maxCapacity = 5000)
  private[relation] val friends = AsyncCache(findFriends, maxCapacity = 50000)
  private[relation] val relation = AsyncCache(findRelation, maxCapacity = 50000)

  private def findFriends(userId: String): Fu[Set[ID]] =
    following(userId) flatMap { ids ⇒
      (ids.toList map { id ⇒ following(id) map (_ contains userId) }).sequenceFu map { ids zip _ } map {
        _.filter(_._2).map(_._1)
      }
    }

  private def findRelation(pair: (String, String)): Fu[Option[Relation]] = pair match {
    case (u1, u2) ⇒ following(u1) flatMap { f ⇒
      f(u2).fold(fuccess(true.some), blocking(u1) map { b ⇒
        b(u2).fold(false.some, none)
      })
    }
  }

  private[relation] def invalidate(u1: ID, u2: ID): Funit =
    (List(followers, following, blockers, blocking, friends) flatMap { cache ⇒
      List(u1, u2) map cache.remove
    }).sequenceFu.void >> relation.remove(u1, u2)
}
