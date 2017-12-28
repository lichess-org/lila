package lila.streamer

import lila.db.dsl._
import reactivemongo.bson._

private[streamer] object BsonHandlers {

  implicit val StreamerIdBSONHandler = stringAnyValHandler[Streamer.Id](_.value, Streamer.Id.apply)
  implicit val StreamerListedBSONHandler = booleanAnyValHandler[Streamer.Listed](_.value, Streamer.Listed.apply)
  implicit val StreamerAutoFeaturedBSONHandler = booleanAnyValHandler[Streamer.AutoFeatured](_.value, Streamer.AutoFeatured.apply)
  implicit val StreamerPicturePathBSONHandler = stringAnyValHandler[Streamer.PicturePath](_.value, Streamer.PicturePath.apply)
  implicit val StreamerDescriptionBSONHandler = stringAnyValHandler[Streamer.Description](_.value, Streamer.Description.apply)

  import Streamer.{ Live, Twitch, YouTube }
  implicit val StreamerLiveBSONHandler = Macros.handler[Live]
  implicit val StreamerTwitchBSONHandler = Macros.handler[Twitch]
  implicit val StreamerYouTubeBSONHandler = Macros.handler[YouTube]
  implicit val StreamerBSONHandler = Macros.handler[Streamer]
}
