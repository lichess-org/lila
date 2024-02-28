package lila.fide

import reactivemongo.api.bson.*
import lila.db.dsl.{ given, * }
import chess.FideId

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val federationColl: Coll
)(using Executor):

  object player:
    given handler: BSONDocumentHandler[FidePlayer] = Macros.handler
    val selectActive: Bdoc                         = $doc("deleted" $ne true, "inactive" $ne true)
    def fetch(id: FideId): Fu[Option[FidePlayer]]  = playerColl.byId[FidePlayer](id)

  object federation:
    given BSONDocumentHandler[Federation.Stats]    = Macros.handler
    given handler: BSONDocumentHandler[Federation] = Macros.handler
    def upsert(fed: Federation): Funit =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)
