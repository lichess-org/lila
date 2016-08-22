package lila.coach

case class CoachProfile(
  headline: Option[String] = None,
  description: Option[CoachProfile.Markdown] = None,
  playingExperience: Option[CoachProfile.Markdown] = None,
  teachingExperience: Option[CoachProfile.Markdown] = None,
  otherExperience: Option[CoachProfile.Markdown] = None,
  skills: Option[CoachProfile.Markdown] = None,
  methodology: Option[CoachProfile.Markdown] = None)

object CoachProfile {

  case class Markdown(value: String) extends AnyVal with StringValue
}
