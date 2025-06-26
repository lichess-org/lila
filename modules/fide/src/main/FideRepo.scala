package lila.fide

import chess.FideId
import reactivemongo.api.bson.*

import lila.core.fide as hub
import lila.db.dsl.{ *, given }
import scala.util.Success

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val federationColl: Coll,
    private[fide] val followerColl: Coll
)(using Executor):

  object player:
    given handler: BSONDocumentHandler[FidePlayer]    = Macros.handler
    val selectActive: Bdoc                            = $doc("inactive".$ne(true))
    def selectFed(fed: hub.Federation.Id): Bdoc       = $doc("fed" -> fed)
    def sortStandard: Bdoc                            = $sort.desc("standard")
    def fetch(id: FideId): Fu[Option[FidePlayer]]     = playerColl.byId[FidePlayer](id)
    def fetch(ids: Seq[FideId]): Fu[List[FidePlayer]] =
      playerColl.find($inIds(ids)).cursor[FidePlayer](ReadPref.sec).listAll()
    def countAll = playerColl.count()

  object federation:
    given BSONDocumentHandler[hub.Federation.Stats] = Macros.handler
    given handler: BSONDocumentHandler[Federation]  = Macros.handler
    def upsert(fed: Federation): Funit              =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: hub.Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)

  object follower:
    def followers(playerId: FideId): Fu[Set[UserId]]               = get(playerId)
    def follow(userId: UserId, playerId: FideId): Funit            = save(userId, playerId)
    def unfollow(userId: UserId, playerId: FideId): Funit          = remove(userId, playerId)
    def isFollowing(userId: UserId, playerId: FideId): Fu[Boolean] = exist(userId, playerId)

    private def makeId(u: UserId, p: FideId) = s"$u/$p"
    private def get(p: FideId)               = followerColl.distinctEasy[UserId, Set]("u", $doc("p" -> p))
    private def save(u: UserId, p: FideId)   = playerColl.exists($id(p)).flatMap {
      case true =>
        followerColl.update
          .one(
            $id(makeId(u, p)),
            $doc("u" -> u, "p" -> p),
            upsert = true
          )
          .void
      case false => Future.unit
    }
    private def remove(u: UserId, p: FideId) = followerColl.delete.one($id(makeId(u, p))).void
    private def exist(u: UserId, p: FideId)  = followerColl.exists($id(makeId(u, p)))
