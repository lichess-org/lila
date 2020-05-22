package lila.coach

case class CoachProfile(
    headline: Option[String] = None,
    languages: Option[String] = None,
    hourlyRate: Option[String] = None,
    description: Option[CoachProfile.RichText] = None,
    playingExperience: Option[CoachProfile.RichText] = None,
    teachingExperience: Option[CoachProfile.RichText] = None,
    otherExperience: Option[CoachProfile.RichText] = None,
    skills: Option[CoachProfile.RichText] = None,
    methodology: Option[CoachProfile.RichText] = None,
    youtubeVideos: Option[String] = None,
    youtubeChannel: Option[String] = None,
    publicStudies: Option[String] = None
) {

  lazy val youtubeUrls = youtubeVideos ?? UrlList.youtube.apply

  lazy val studyIds = publicStudies ?? UrlList.study.apply
}

object CoachProfile {

  case class RichText(value: String) extends AnyVal with StringValue
}
