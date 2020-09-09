package lila.tournament

import lila.user.User
import java.util.concurrent.ConcurrentHashMap

case class UserIdWithMagicScore(userId: User.ID, magicScore: Int) extends Ordered[UserIdWithMagicScore] {
  override def compare(that: UserIdWithMagicScore) =
    if (magicScore > that.magicScore) -1
    else if (magicScore < that.magicScore) 1
    else userId compare that.userId
}

class UpdatableRanking(
    magicScores: scala.collection.immutable.Map[User.ID, Int],
    ranks: scala.collection.immutable.TreeSet[UserIdWithMagicScore]
) {
  private[this] def updatedRanks(userId: User.ID, oldScore: Int, newScore: Int) =
    ranks excl UserIdWithMagicScore(userId, oldScore) incl UserIdWithMagicScore(userId, newScore)

  def updated(userId: User.ID, magicScore: Int): UpdatableRanking =
    magicScores
      .get(userId)
      .fold(
        new UpdatableRanking(
          magicScores.updated(userId, magicScore),
          ranks incl UserIdWithMagicScore(userId, magicScore)
        )
      )(oldScore => {
        if (oldScore != magicScore)
          new UpdatableRanking(
            magicScores.updated(userId, magicScore),
            updatedRanks(userId, oldScore, magicScore)
          )
        else this
      })

  def ++(that: IterableOnce[UserIdWithMagicScore]): UpdatableRanking =
    that.iterator.foldLeft(this)((acc, x) => acc.updated(x.userId, x.magicScore))

  def getRank(userId: User.ID): Option[Int] =
    magicScores.get(userId).map(m => ranks.rangeUntil(UserIdWithMagicScore("", m)).size)
}

object UpdatableRanking {
  def apply(it: Seq[UserIdWithMagicScore]) =
    new UpdatableRanking(
      it.map(p => (p.userId, p.magicScore)).toMap,
      scala.collection.immutable.TreeSet(it: _*)
    )
}

class RankingStore {
  private val byTourId = new ConcurrentHashMap[Tournament.ID, UpdatableRanking]
  def update(tourId: Tournament.ID, userId: User.ID, magicScore: Int): Unit =
    byTourId.computeIfPresent(
      tourId,
      (_: Tournament.ID, v: UpdatableRanking) => v.updated(userId, magicScore)
    )
  def updateAll(tourId: Tournament.ID, userIds: IterableOnce[UserIdWithMagicScore]): Unit =
    byTourId.computeIfPresent(tourId, (_: Tournament.ID, v: UpdatableRanking) => v ++ userIds)
}
