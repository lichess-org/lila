package lila.practice

import io.methvin.play.autoconfig.*
import play.api.ConfigLoader

import lila.common.config.*
import lila.study.Study

final class PracticeConfig(
    val sections: List[PracticeConfigSection]
):

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id

object PracticeConfig:
  val empty = new PracticeConfig(Nil)

  private given ConfigLoader[PracticeConfigStudy]   = AutoConfig.loader[PracticeConfigStudy]
  private given ConfigLoader[PracticeConfigSection] = AutoConfig.loader[PracticeConfigSection]

  given ConfigLoader[PracticeConfig] = AutoConfig.loader[PracticeConfig]

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
