package lila.recap

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.core.game.Source
import scalalib.Iso

private final class RecapRepo(colls: RecapColls)(using Executor):

  private given BSONHandler[FiniteDuration] =
    BSONIntegerHandler.as[FiniteDuration](_.seconds, _.toSeconds.toInt)

  private given BSONDocumentHandler[NbWin] = Macros.handler
  private given [A: BSONHandler]: BSONHandler[Recap.Counted[A]] = Macros.handler
  private given BSONDocumentHandler[Recap.Perf] = Macros.handler

  private given sourceIso: Iso.StringIso[Source] =
    Iso.string(key => Source.byName.get(key).err(s"Unknown source $key"), _.name)
  private given BSONHandler[Map[Source, Int]] = typedMapHandlerIso[Source, Int]

  private given BSONDocumentHandler[RecapGames] = Macros.handler
  private given BSONDocumentHandler[PuzzleVotes] = Macros.handler
  private given BSONDocumentHandler[RecapPuzzles] = Macros.handler
  private given BSONDocumentHandler[Recap] = Macros.handler

  def get(userId: UserId): Fu[Option[Recap]] = colls.recap.byId[Recap](userId)

  def insert(recap: Recap): Funit =
    colls.recap.insert.one(recap).void.recover(lila.db.ignoreDuplicateKey)
