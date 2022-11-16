package lila.practice

import lila.common.autoconfig.{ *, given }
import play.api.ConfigLoader

import lila.common.config.*
import lila.study.Study

final class PracticeConfig(
    val sections: List[PracticeConfigSection]
):

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id.apply

object PracticeConfig:
  val empty = new PracticeConfig(Nil)

  private given studyLoader: ConfigLoader[PracticeConfigStudy] = AutoConfig.loader
  private given ConfigLoader[PracticeConfigSection]            = AutoConfig.loader

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
    val name: String,
    val desc: String
)
