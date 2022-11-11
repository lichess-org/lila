package lila.streamer

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson._

private object BsonHandlers {

  given BSONHandler[Streamer.Id]               = stringAnyValHandler(_.value, Streamer.Id.apply)
  given BSONHandler[Streamer.Listed]           = booleanAnyValHandler(_.value, Streamer.Listed.apply)
  given BSONHandler[Streamer.Name]             = stringAnyValHandler(_.value, Streamer.Name.apply)
  given BSONHandler[Streamer.Headline]         = stringAnyValHandler(_.value, Streamer.Headline.apply)
  given BSONHandler[Streamer.Description]      = stringAnyValHandler(_.value, Streamer.Description.apply)
  given BSONDocumentHandler[Streamer.Twitch]   = Macros.handler
  given BSONDocumentHandler[Streamer.YouTube]  = Macros.handler
  given BSONDocumentHandler[Streamer.Approval] = Macros.handler
  given BSONDocumentHandler[Streamer]          = Macros.handler
}
