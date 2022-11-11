package lila.coach

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

private[coach] object BsonHandlers:

  given BSONHandler[Coach.Id]              = stringAnyValHandler(_.value, Coach.Id.apply)
  given BSONHandler[Coach.Listed]          = booleanAnyValHandler(_.value, Coach.Listed.apply)
  given BSONHandler[Coach.Available]       = booleanAnyValHandler(_.value, Coach.Available.apply)
  given BSONHandler[CoachProfile.RichText] = stringAnyValHandler(_.value, CoachProfile.RichText.apply)
  given BSONDocumentHandler[CoachProfile]  = Macros.handler
  given BSONDocumentHandler[Coach.User]    = Macros.handler
  given BSONDocumentHandler[Coach]         = Macros.handler
  given BSONDocumentHandler[CoachReview]   = Macros.handler
