package lila.streamer

import lila.db.dsl.given
import reactivemongo.api.bson.*

private object BsonHandlers:

  given BSONDocumentHandler[Streamer.YouTube]  = Macros.handler
  given BSONDocumentHandler[Streamer.Twitch]   = Macros.handler
  given BSONDocumentHandler[Streamer.Approval] = Macros.handler
  given BSONDocumentHandler[Streamer]          = Macros.handler
