package lila.relation

import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }
import lila.relation.RelationRepo.makeId

final class SubscriptionRepo(colls: Colls, userRepo: lila.user.UserRepo)(using
    Executor
) {
  val coll = colls.subscription

  // for streaming, streamerId is the user UserId of the streamer being subscribed to
  def subscribersOnlineSince(streamerId: UserId, daysAgo: Int): Fu[List[UserId]] =
    coll
      .aggregateOne(readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match($doc("s" -> streamerId)) -> List(
          PipelineOperator(
            $lookup.pipeline(
              from = userRepo.coll,
              as = "user",
              local = "u",
              foreign = "_id",
              pipe = List(
                $doc("$match"   -> $expr($doc("$gt" -> $arr("$seenAt", nowInstant.minusDays(daysAgo))))),
                $doc("$project" -> $id(true))
              )
            )
          ),
          Match("user" $ne $arr()),
          Group(BSONNull)(
            "ids" -> PushField("u")
          )
        )
      }
      .map(~_.flatMap(_.getAsOpt[List[UserId]]("ids")))

  def subscribe(userId: UserId, streamerId: UserId): Funit =
    coll.update
      .one(
        $id(makeId(userId, streamerId)),
        $doc("u" -> userId, "s" -> streamerId),
        upsert = true
      )
      .void

  def unsubscribe(userId: UserId, streamerId: UserId): Funit =
    coll.delete.one($id(makeId(userId, streamerId))).void

  def isSubscribed[U, S](userId: U, streamerId: S)(using
      idOfU: UserIdOf[U],
      idOfS: UserIdOf[S]
  ): Fu[Boolean] =
    coll.exists($id(makeId(idOfU(userId), idOfS(streamerId))))

  // only use "_id", not "s", so that mongo can work entirely from the index
  def filterSubscribed(subscriber: UserId, streamerIds: List[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[String, Set]("_id", $inIds(streamerIds.map(makeId(subscriber, _)))) map { ids =>
      UserId from ids.flatMap(_.split('/').lift(1))
    }
}
