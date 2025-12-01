package lila.fide

import chess.FideId
import reactivemongo.api.bson.*

import lila.core.fide as hub
import lila.db.dsl.{ *, given }
import scala.util.Success

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val federationColl: Coll,
    private val followerColl: Coll
)(using Executor):

  object player:
    given handler: BSONDocumentHandler[FidePlayer] = Macros.handler
    val selectActive: Bdoc = $doc("inactive".$ne(true))
    def selectFed(fed: hub.Federation.Id): Bdoc = $doc("fed" -> fed)
    def sortStandard: Bdoc = $sort.desc("standard")
    def fetch(id: FideId): Fu[Option[FidePlayer]] = playerColl.byId[FidePlayer](id)
    def fetch(ids: Seq[FideId]): Fu[List[FidePlayer]] =
      playerColl.find($inIds(ids)).cursor[FidePlayer](ReadPref.sec).listAll()
    def countAll = playerColl.count()

  object federation:
    given BSONDocumentHandler[hub.Federation.Stats] = Macros.handler
    given handler: BSONDocumentHandler[Federation] = Macros.handler
    def upsert(fed: Federation): Funit =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: hub.Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)

  object follower:
    // { _id: '14204118/thibault', u: 'thibault' }
    type FollowId = String
    private object followId:
      def make(p: FideId, u: UserId) = s"$p/$u"
      def toUserId(id: FollowId): UserId = UserId(id.drop(id.indexOf('/') + 1))
      def toFideId(id: FollowId): FideId = FideId(id.takeWhile(_ != '/').toInt)

    def followers(p: FideId): Fu[Set[UserId]] = (p.value > 0).so:
      for ids <- followerColl.distinctEasy[FollowId, Set]("_id", "_id".$startsWith(s"$p/"))
      yield ids.map(followId.toUserId)
    def follow(u: UserId, p: FideId) = playerColl
      .exists($id(p))
      .flatMapz:
        followerColl.update.one($id(followId.make(p, u)), $doc("u" -> u), upsert = true).void
    def unfollow(u: UserId, p: FideId) = followerColl.delete.one($id(followId.make(p, u))).void
    def isFollowing(u: UserId, p: FideId) = followerColl.exists($id(followId.make(p, u)))

    def withFollows(players: Seq[FidePlayer], u: UserId): Fu[Seq[FidePlayer.WithFollow]] =
      val ids = players.map(_.id).map(followId.make(_, u))
      followerColl
        .distinctEasy[FollowId, Set]("_id", "_id".$in(ids))
        .map(_.map(followId.toFideId))
        .map: followedIds =>
          players.map: p =>
            FidePlayer.WithFollow(p, followedIds.contains(p.id))
