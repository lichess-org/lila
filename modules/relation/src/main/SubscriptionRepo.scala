package lila.relation

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class SubscriptionRepo(colls: Colls, userRepo: lila.core.user.UserRepo)(using
    Executor
) extends lila.core.relation.SubscriptionRepo:

  private val coll = colls.subscription

  // for streaming, streamerId is the user UserId of the streamer being subscribed to
  def subscribersOnlineSince(streamerId: UserId, daysAgo: Int): Fu[List[UserId]] =
    coll
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc("s" -> streamerId)) -> List(
          PipelineOperator(
            $lookup.pipeline(
              from = userRepo.coll,
              as = "user",
              local = "u",
              foreign = "_id",
              pipe = List(
                $doc("$match" -> $expr($doc("$gt" -> $arr("$seenAt", nowInstant.minusDays(daysAgo))))),
                $doc("$project" -> $id(true))
              )
            )
          ),
          Match("user".$ne($arr())),
          Group(BSONNull)(
            "ids" -> PushField("u")
          )
        )
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

  def isSubscribed[U: UserIdOf, S: UserIdOf](userId: U, streamerId: S): Fu[Boolean] =
    coll.exists($id(makeId(userId.id, streamerId.id)))

  // only use "_id", not "s", so that mongo can work entirely from the index
  def filterSubscribed(subscriber: UserId, streamerIds: List[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[String, Set]("_id", $inIds(streamerIds.map(makeId(subscriber, _)))).map { ids =>
      UserId.from(ids.flatMap(_.split('/').lift(1)))
    }
