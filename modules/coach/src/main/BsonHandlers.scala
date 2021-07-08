package lila.coach

import reactivemongo.api.bson._

import lila.db.dsl._

private[coach] object BsonHandlers {

  implicit val CoachIdBSONHandler     = stringAnyValHandler[Coach.Id](_.value, Coach.Id.apply)
  implicit val CoachListedBSONHandler = booleanAnyValHandler[Coach.Listed](_.value, Coach.Listed.apply)
  implicit val CoachAvailableBSONHandler =
    booleanAnyValHandler[Coach.Available](_.value, Coach.Available.apply)
  implicit val CoachPicturePathBSONHandler =
    stringAnyValHandler[Coach.PicturePath](_.value, Coach.PicturePath.apply)

  implicit val CoachProfileRichTextBSONHandler =
    stringAnyValHandler[CoachProfile.RichText](_.value, CoachProfile.RichText.apply)
  implicit val CoachProfileBSONHandler = Macros.handler[CoachProfile]

  import Coach.User
  implicit val CoachUserBSONHandler = Macros.handler[User]

  implicit val CoachBSONHandler = Macros.handler[Coach]

  implicit val CoachReviewBSONHandler = Macros.handler[CoachReview]
}
