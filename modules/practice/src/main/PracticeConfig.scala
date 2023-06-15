package lila.practice

import lila.common.autoconfig.{ *, given }
import play.api.ConfigLoader

final class PracticeConfig(val sections: List[PracticeConfigSection]):

  def studyIds = sections.flatMap(_.studies.map(_.id)) map { StudyId(_) }

object PracticeConfig:
  val empty = PracticeConfig(Nil)

  private given [A](using ConfigLoader[A]): ConfigLoader[Option[A]] = optionalConfig[A]
  private given studyLoader: ConfigLoader[PracticeConfigStudy]      = AutoConfig.loader
  private given ConfigLoader[PracticeConfigSection]                 = AutoConfig.loader

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
