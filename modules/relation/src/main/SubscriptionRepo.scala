package lila.relation

import org.joda.time.DateTime
import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }
import lila.relation.RelationRepo.makeId
import lila.user.User

final class SubscriptionRepo(colls: Colls, userRepo: lila.user.UserRepo)(implicit
    ec: scala.concurrent.ExecutionContext
) {
  val coll = colls.subscription

  // for streaming, feedId is the user id of the streamer being subscribed to
  def subscribersOnlineSince(streamerId: ID, daysAgo: Int): Fu[List[User.ID]] =
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
                $doc("$match"   -> $expr($doc("$gt" -> $arr("$seenAt", DateTime.now.minusDays(daysAgo))))),
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
      .map(~_.flatMap(_.getAsOpt[List[User.ID]]("ids")))

  def subscribe(userId: ID, streamerId: ID): Funit =
    coll.update
      .one(
        $id(makeId(userId, streamerId)),
        $doc("u" -> userId, "s" -> streamerId),
        upsert = true
      )
      .void

  def unsubscribe(userId: ID, streamerId: ID): Funit =
    coll.delete.one($id(makeId(userId, streamerId))).void

  def isSubscribed(userId: ID, streamerId: ID): Fu[Boolean] =
    coll.exists($id(makeId(userId, streamerId)))

  def isSubscribed(userId: ID, streamerIds: List[ID]): Fu[Map[User.ID, Boolean]] = {
    coll
      .find(
        $inIds(streamerIds map (makeId(userId, _))),
        $doc("s" -> true, "_id" -> false).some
      )
      .cursor[Bdoc]()
      .list(-1)
      .dmap { x =>
        val subscribedTo = x flatMap (_ string "s")
        streamerIds.map(s => (s, subscribedTo contains s)).toMap
      }
  }
}
