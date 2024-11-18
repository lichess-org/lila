package lila.recap

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }

private final class RecapRepo(colls: RecapColls)(using Executor):

  private given BSONHandler[FiniteDuration] =
    BSONIntegerHandler.as[FiniteDuration](_.seconds, _.toSeconds.toInt)

  private given BSONDocumentHandler[Recap.Results]              = Macros.handler
  private given [A: BSONHandler]: BSONHandler[Recap.Counted[A]] = Macros.handler
  private given BSONDocumentHandler[Recap.Perf]                 = Macros.handler
  private given BSONDocumentHandler[Recap]                      = Macros.handler

  def get(userId: UserId): Fu[Option[Recap]] = colls.recap.byId[Recap](userId)

  def insert(recap: Recap): Funit =
    colls.recap.insert.one(recap).void.recover(lila.db.ignoreDuplicateKey)
