package lila.fide

import chess.FideId
import reactivemongo.api.bson.*

import lila.core.fide as hub
import lila.db.dsl.{ *, given }
import scala.util.Success

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val federationColl: Coll
)(using Executor):

  object player:
    given handler: BSONDocumentHandler[FidePlayer]    = Macros.handler
    val selectActive: Bdoc                            = $doc("inactive".$ne(true))
    def selectFed(fed: hub.Federation.Id): Bdoc       = $doc("fed" -> fed)
    def sortStandard: Bdoc                            = $sort.desc("standard")
    def fetch(id: FideId): Fu[Option[FidePlayer]]     = playerColl.byId[FidePlayer](id)
    def fetch(ids: Seq[FideId]): Fu[List[FidePlayer]] =
      playerColl.find($inIds(ids)).cursor[FidePlayer](ReadPref.sec).listAll()
    def fetchWithSubs(ids: Seq[FideId]): Fu[List[(FidePlayer, Set[UserId])]] =
      playerColl.find($inIds(ids)).cursor[BSONDocument](ReadPref.sec).listAll().map { players =>
        players
          .map(player =>
            for
              p    <- BSON.readDocument[FidePlayer](player)
              subs <- player.getAsTry[Set[UserId]]("subscribers")
            yield (p, subs)
          )
          .collect { case Success(value) => value }
      }
    def countAll                                  = playerColl.count()
    def subscribers(fid: FideId): Fu[Set[UserId]] =
      playerColl.distinctEasy[UserId, Set]("subscribers", $id(fid))
    def setSubscribed(fid: FideId, uid: UserId, isSubscribed: Boolean): Funit =
      playerColl.update
        .one($id(fid), if isSubscribed then $addToSet("subscribers" -> uid) else $pull("subscribers" -> uid))
        .void
    def isSubscribed(fid: FideId, uid: UserId): Fu[Boolean] =
      playerColl.exists($doc($id(fid), "subscribers" -> uid))

  object federation:
    given BSONDocumentHandler[hub.Federation.Stats] = Macros.handler
    given handler: BSONDocumentHandler[Federation]  = Macros.handler
    def upsert(fed: Federation): Funit              =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: hub.Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)
