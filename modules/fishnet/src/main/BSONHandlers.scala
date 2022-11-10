package lila.fishnet

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

import chess.variant.Variant

private object BSONHandlers:

  given BSONHandler[Client.Key]     = stringAnyValHandler(_.value, Client.Key.apply)
  given BSONHandler[Client.Version] = stringAnyValHandler(_.value, Client.Version.apply)
  given BSONHandler[Client.Python]  = stringAnyValHandler(_.value, Client.Python.apply)
  given BSONHandler[Client.UserId]  = stringAnyValHandler(_.value, Client.UserId.apply)

  given BSONHandler[Client.Skill] = tryHandler(
    { case BSONString(v) => Client.Skill byKey v toTry s"Invalid client skill $v" },
    x => BSONString(x.key)
  )

  given BSONDocumentHandler[Client.Instance] = Macros.handler

  given BSONDocumentHandler[Client] = Macros.handler

  given BSONHandler[Variant] = tryHandler(
    { case BSONInteger(v) => Variant(v) toTry s"Invalid variant $v" },
    x => BSONInteger(x.id)
  )

  given BSONHandler[Work.Id]                       = stringAnyValHandler[Work.Id](_.value, Work.Id.apply)
  private given BSONDocumentHandler[Work.Acquired] = Macros.handler
  private given BSONDocumentHandler[Work.Clock]    = Macros.handler
  private given BSONDocumentHandler[Work.Game]     = Macros.handler
  private given BSONDocumentHandler[Work.Sender]   = Macros.handler
  given BSONDocumentHandler[Work.Analysis]         = Macros.handler
