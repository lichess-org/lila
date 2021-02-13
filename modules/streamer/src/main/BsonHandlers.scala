package lila.streamer

import lila.db.dsl._
import reactivemongo.api.bson._

private object BsonHandlers {

  implicit val StreamerIdBSONHandler = stringAnyValHandler[Streamer.Id](_.value, Streamer.Id.apply)
  implicit val StreamerListedBSONHandler =
    booleanAnyValHandler[Streamer.Listed](_.value, Streamer.Listed.apply)
  implicit val StreamerPicturePathBSONHandler =
    stringAnyValHandler[Streamer.PicturePath](_.value, Streamer.PicturePath.apply)
  implicit val StreamerNameBSONHandler = stringAnyValHandler[Streamer.Name](_.value, Streamer.Name.apply)
  implicit val StreamerHeadlineBSONHandler =
    stringAnyValHandler[Streamer.Headline](_.value, Streamer.Headline.apply)
  implicit val StreamerDescriptionBSONHandler =
    stringAnyValHandler[Streamer.Description](_.value, Streamer.Description.apply)

  import Streamer.{ Approval, Twitch, YouTube }
  implicit val StreamerTwitchBSONHandler   = Macros.handler[Twitch]
  implicit val StreamerYouTubeBSONHandler  = Macros.handler[YouTube]
  implicit val StreamerApprovalBSONHandler = Macros.handler[Approval]
  implicit val StreamerBSONHandler         = Macros.handler[Streamer]
}
