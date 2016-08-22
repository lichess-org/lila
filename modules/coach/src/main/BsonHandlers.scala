package lila.coach

import lila.db.dsl._
import reactivemongo.bson._

private[coach] object BsonHandlers {

  implicit val CoachIdBSONHandler = stringAnyValHandler[Coach.Id](_.value, Coach.Id.apply)
  implicit val CoachEnabledBSONHandler = booleanAnyValHandler[Coach.Enabled](_.value, Coach.Enabled.apply)
  implicit val CoachAvailableBSONHandler = booleanAnyValHandler[Coach.Available](_.value, Coach.Available.apply)
  implicit val CoachCentsBSONHandler = intAnyValHandler[Coach.Cents](_.value, Coach.Cents.apply)

  implicit val CoachProfileMarkdownBSONHandler = stringAnyValHandler[CoachProfile.Markdown](_.value, CoachProfile.Markdown.apply)
  implicit val CoachProfileBSONHandler = Macros.handler[CoachProfile]

  implicit val CoachBSONHandler = Macros.handler[Coach]
}
