package lila.core
package relation

type Relation = Boolean
val Follow: Relation = true
val Block: Relation  = false

case class Relations(in: Option[Relation], out: Option[Relation])

abstract class RelationApi(val coll: reactivemongo.api.bson.collection.BSONCollection):
  def fetchRelation(u1: UserId, u2: UserId): Fu[Option[Relation]]
  def fetchRelations(u1: UserId, u2: UserId): Fu[Relations]
  def fetchFriends(userId: UserId): Fu[Set[UserId]]
  def fetchAreFriends(u1: UserId, u2: UserId): Fu[Boolean]
  def fetchFollows(u1: UserId, u2: UserId): Fu[Boolean]
  def fetchBlocks(u1: UserId, u2: UserId): Fu[Boolean]
  def filterBlocked(by: UserId, candidates: Iterable[UserId]): Fu[Set[UserId]]
  def searchFollowedBy(u: UserId, term: UserSearch, max: Int): Fu[List[UserId]]
  def freshFollowersFromSecondary(userId: UserId): Fu[List[UserId]]

trait SubscriptionRepo:
  def isSubscribed[U: UserIdOf, S: UserIdOf](userId: U, streamerId: S): Fu[Boolean]
  def filterSubscribed(subscriber: UserId, streamerIds: List[UserId]): Fu[Set[UserId]]
  def subscribersOnlineSince(streamerId: UserId, daysAgo: Int): Fu[List[UserId]]
