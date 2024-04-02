package lila.fide

import chess.FideId
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.core.{ fide as hub }

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val federationColl: Coll
)(using Executor):

  object player:
    given handler: BSONDocumentHandler[FidePlayer] = Macros.handler
    val selectActive: Bdoc                         = $doc("deleted".$ne(true), "inactive".$ne(true))
    def selectFed(fed: hub.Federation.Id): Bdoc    = $doc("fed" -> fed)
    def sortStandard: Bdoc                         = $sort.desc("standard")
    def fetch(id: FideId): Fu[Option[FidePlayer]]  = playerColl.byId[FidePlayer](id)

  object federation:
    given BSONDocumentHandler[hub.Federation.Stats] = Macros.handler
    given handler: BSONDocumentHandler[Federation]  = Macros.handler
    def upsert(fed: Federation): Funit =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: hub.Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)
