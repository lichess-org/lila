package lila.fide

import chess.FideId
import reactivemongo.api.bson.*

import lila.core.fide as hub
import lila.db.dsl.{ *, given }

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val federationColl: Coll
)(using Executor):

  object player:
    given handler: BSONDocumentHandler[FidePlayer] = Macros.handler
    val selectActive: Bdoc                         = $doc("inactive".$ne(true))
    def selectFed(fed: hub.Federation.Id): Bdoc    = $doc("fed" -> fed)
    def sortStandard: Bdoc                         = $sort.desc("standard")
    def fetch(id: FideId): Fu[Option[FidePlayer]]  = playerColl.byId[FidePlayer](id)
    def fetch(ids: Seq[FideId]): Fu[List[FidePlayer]] =
      playerColl.find($inIds(ids)).cursor[FidePlayer](ReadPref.sec).listAll()
    def countAll = playerColl.count()

  object federation:
    given BSONDocumentHandler[hub.Federation.Stats] = Macros.handler
    given handler: BSONDocumentHandler[Federation]  = Macros.handler
    def upsert(fed: Federation): Funit =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: hub.Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)
