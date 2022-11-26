package lila.coach

case class CoachProfile(
    headline: Option[String] = None,
    hourlyRate: Option[String] = None,
    description: Option[RichText] = None,
    playingExperience: Option[RichText] = None,
    teachingExperience: Option[RichText] = None,
    otherExperience: Option[RichText] = None,
    skills: Option[RichText] = None,
    methodology: Option[RichText] = None,
    youtubeVideos: Option[String] = None,
    youtubeChannel: Option[String] = None,
    publicStudies: Option[String] = None
):

  lazy val youtubeUrls = youtubeVideos ?? UrlList.youtube.apply

  lazy val studyIds = publicStudies ?? UrlList.study.apply

  def textLines: List[String] = List(
    "headline"           -> headline,
    "hourlyRate"         -> hourlyRate,
    "description"        -> description.map(_.value),
    "playingExperience"  -> playingExperience.map(_.value),
    "teachingExperience" -> teachingExperience.map(_.value),
    "otherExperience"    -> otherExperience.map(_.value),
    "skills"             -> skills.map(_.value),
    "methodology"        -> methodology.map(_.value),
    "youtubeVideos"      -> youtubeVideos,
    "youtubeChannel"     -> youtubeChannel,
    "publicStudies"      -> publicStudies
  ) collect { case (k, Some(v)) =>
    s"$k: $v"
  }
