package lila.streamer

import lila.db.BSON
import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

private object BsonHandlers:

  given BSONDocumentHandler[Streamer.YouTube] = new BSON[Streamer.YouTube]:
    def reads(r: BSON.Reader) =
      Streamer.YouTube(
        r.get[String]("channelId"),
        r.getO[String]("liveVideoId"),
        r.getO[String]("pubsubVideoId")
      )
    def writes(w: BSON.Writer, o: Streamer.YouTube) =
      $doc("channelId" -> o.channelId, "liveVideoId" -> o.liveVideoId, "pubsubVideoId" -> o.pubsubVideoId)

  given BSONDocumentHandler[Streamer.Twitch]   = Macros.handler
  given BSONDocumentHandler[Streamer.Approval] = Macros.handler
  given BSONDocumentHandler[Streamer]          = Macros.handler
