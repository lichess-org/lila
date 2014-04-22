package lila.relation

import lila.memo.AsyncCache

private[relation] final class Cached {

  private[relation] val followers = AsyncCache(RelationRepo.followers, maxCapacity = 8000)
  private[relation] val following = AsyncCache(RelationRepo.following, maxCapacity = 8000)
  private[relation] val blockers = AsyncCache(RelationRepo.blockers, maxCapacity = 8000)
  private[relation] val blocking = AsyncCache(RelationRepo.blocking, maxCapacity = 8000)
  private[relation] val relation = AsyncCache(findRelation, maxCapacity = 80000)

  private def findRelation(pair: (String, String)): Fu[Option[Relation]] = pair match {
    case (u1, u2) => following(u1) flatMap { f =>
      f(u2).fold(fuccess(true.some), blocking(u1) map { b =>
        b(u2).fold(false.some, none)
      })
    }
  }

  private[relation] def invalidate(u1: ID, u2: ID): Funit =
    (List(followers, following, blockers, blocking) flatMap { cache =>
      List(u1, u2) map cache.remove
    }).sequenceFu.void >> relation.remove(u1, u2)
}
