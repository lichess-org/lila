package lila.fishnet

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

import chess.variant.Variant

private object BSONHandlers:

  given BSONHandler[Client.Skill] = tryHandler(
    { case BSONString(v) => Client.Skill byKey v toTry s"Invalid client skill $v" },
    x => BSONString(x.key)
  )

  given BSONDocumentHandler[Client.Instance] = Macros.handler

  given BSONDocumentHandler[Client] = Macros.handler

  given BSONHandler[Variant] = variantByIdHandler

  private given BSONDocumentHandler[Work.Acquired] = Macros.handler
  private given BSONDocumentHandler[Work.Game]     = Macros.handler
  private given BSONDocumentHandler[Work.Sender]   = Macros.handler
  given BSONDocumentHandler[Work.Analysis]         = Macros.handler
