package lila.streamer

import reactivemongo.api.bson._

import lila.db.dsl._

private object BsonHandlers {

  implicit val StreamerIdBSONHandler: BSONHandler[Streamer.Id] =
    stringAnyValHandler[Streamer.Id](_.value, Streamer.Id.apply)
  implicit val StreamerListedBSONHandler: BSONHandler[Streamer.Listed] =
    booleanAnyValHandler[Streamer.Listed](_.value, Streamer.Listed.apply)
  implicit val StreamerPicturePathBSONHandler: BSONHandler[Streamer.PicturePath] =
    stringAnyValHandler[Streamer.PicturePath](_.value, Streamer.PicturePath.apply)
  implicit val StreamerNameBSONHandler: BSONHandler[Streamer.Name] =
    stringAnyValHandler[Streamer.Name](_.value, Streamer.Name.apply)
  implicit val StreamerHeadlineBSONHandler: BSONHandler[Streamer.Headline] =
    stringAnyValHandler[Streamer.Headline](_.value, Streamer.Headline.apply)
  implicit val StreamerDescriptionBSONHandler: BSONHandler[Streamer.Description] =
    stringAnyValHandler[Streamer.Description](_.value, Streamer.Description.apply)

  import Streamer.Approval
  import Streamer.Twitch
  import Streamer.YouTube
  implicit val StreamerTwitchBSONHandler: BSONDocumentHandler[Twitch]     = Macros.handler[Twitch]
  implicit val StreamerYouTubeBSONHandler: BSONDocumentHandler[YouTube]   = Macros.handler[YouTube]
  implicit val StreamerApprovalBSONHandler: BSONDocumentHandler[Approval] = Macros.handler[Approval]
  implicit val StreamerBSONHandler: BSONDocumentHandler[Streamer]         = Macros.handler[Streamer]
}
