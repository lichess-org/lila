package lila.recap

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class RecapApi(colls: RecapColls, queue: lila.memo.ParallelMongoQueue[UserId])(using Executor):

  private given BSONHandler[FiniteDuration] =
    BSONIntegerHandler.as[FiniteDuration](_.seconds, _.toSeconds.toInt)
  private given BSONDocumentHandler[Recap.Results] = Macros.handler
  private given BSONDocumentHandler[Recap]         = Macros.handler

  def get(userId: UserId): Fu[Option[Recap]] =
    colls.recap.byId[Recap](userId)
