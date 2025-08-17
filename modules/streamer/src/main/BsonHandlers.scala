package lila.streamer

import reactivemongo.api.bson.*

import lila.db.dsl.given

private object BsonHandlers:

  given BSONDocumentHandler[Streamer.YouTube] = Macros.handler
  given BSONDocumentHandler[Streamer.Twitch] = Macros.handler
  given BSONDocumentHandler[Streamer.Approval] = Macros.handler
  given BSONDocumentHandler[Streamer] = Macros.handler
