package lila.coach

import reactivemongo.api.bson._

import lila.db.dsl._

private[coach] object BsonHandlers {

  implicit val CoachIdBSONHandler: BSONHandler[Coach.Id]     = stringAnyValHandler[Coach.Id](_.value, Coach.Id.apply)
  implicit val CoachListedBSONHandler: BSONHandler[Coach.Listed] = booleanAnyValHandler[Coach.Listed](_.value, Coach.Listed.apply)
  implicit val CoachAvailableBSONHandler: BSONHandler[Coach.Available] =
    booleanAnyValHandler[Coach.Available](_.value, Coach.Available.apply)
  implicit val CoachApprovedBSONHandler: BSONHandler[Coach.Approved] = booleanAnyValHandler[Coach.Approved](_.value, Coach.Approved.apply)
  implicit val CoachPicturePathBSONHandler: BSONHandler[Coach.PicturePath] =
    stringAnyValHandler[Coach.PicturePath](_.value, Coach.PicturePath.apply)

  implicit val CoachProfileRichTextBSONHandler: BSONHandler[CoachProfile.RichText] =
    stringAnyValHandler[CoachProfile.RichText](_.value, CoachProfile.RichText.apply)
  implicit val CoachProfileBSONHandler: BSONDocumentHandler[CoachProfile] = Macros.handler[CoachProfile]

  import Coach.User
  implicit val CoachUserBSONHandler: BSONDocumentHandler[User] = Macros.handler[User]

  implicit val CoachBSONHandler: BSONDocumentHandler[Coach] = Macros.handler[Coach]

}
