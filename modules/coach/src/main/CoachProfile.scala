package lila.coach

import lila.core.data.RichText

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

  lazy val youtubeUrls = youtubeVideos.so(UrlList.youtube.apply)

  lazy val studyIds = publicStudies.so(UrlList.study.apply)

  def textLines: List[String] = List(
    "headline"           -> headline,
    "hourlyRate"         -> hourlyRate,
    "description"        -> description,
    "playingExperience"  -> playingExperience,
    "teachingExperience" -> teachingExperience,
    "otherExperience"    -> otherExperience,
    "skills"             -> skills,
    "methodology"        -> methodology,
    "youtubeVideos"      -> youtubeVideos,
    "youtubeChannel"     -> youtubeChannel,
    "publicStudies"      -> publicStudies
  ).collect { case (k, Some(v)) =>
    s"$k: $v"
  }
