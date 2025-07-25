package lila.practice

import play.api.ConfigLoader

import lila.common.autoconfig.{ *, given }
import lila.core.study.data.StudyName

final class PracticeConfig(val sections: List[PracticeConfigSection]):

  def studyIds = sections.flatMap(_.studies.map(_.id)).map { StudyId(_) }

object PracticeConfig:
  val empty = PracticeConfig(Nil)

  private given studyLoader: ConfigLoader[PracticeConfigStudy] = AutoConfig.loader
  private given ConfigLoader[PracticeConfigSection] = AutoConfig.loader

  given ConfigLoader[PracticeConfig] = AutoConfig.loader

final class PracticeConfigSection(
    val id: String,
    val hide: Option[Boolean] = None,
    val name: String,
    val studies: List[PracticeConfigStudy]
)

final class PracticeConfigStudy(
    val id: String, // study ID
    val hide: Option[Boolean] = None,
    val name: StudyName,
    val desc: String
)
