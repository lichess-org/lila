package lila.practice

import io.methvin.play.autoconfig._
import play.api.ConfigLoader

import lila.common.config._
import lila.study.Study

final class PracticeConfig(
    val sections: List[PracticeConfigSection]
) {

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id.apply
}

object PracticeConfig {
  val empty = new PracticeConfig(Nil)

  implicit private val studyLoader   = AutoConfig.loader[PracticeConfigStudy]
  implicit private val sectionLoader = AutoConfig.loader[PracticeConfigSection]

  implicit val loader = AutoConfig.loader[PracticeConfig]
}

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
