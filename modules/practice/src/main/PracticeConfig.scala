package lila.practice

import play.api.ConfigLoader

import io.methvin.play.autoconfig._

import lila.common.config._
import lila.study.Study

final class PracticeConfig(
    val sections: List[PracticeConfigSection]
) {

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id.apply
}

object PracticeConfig {
  val empty = new PracticeConfig(Nil)

  implicit private val studyLoader: ConfigLoader[PracticeConfigStudy]   = AutoConfig.loader[PracticeConfigStudy]
  implicit private val sectionLoader: ConfigLoader[PracticeConfigSection] = AutoConfig.loader[PracticeConfigSection]
  implicit val loader: ConfigLoader[PracticeConfig]                = AutoConfig.loader[PracticeConfig]
}

final class PracticeConfigSection(
    val id: String,
    val name: String,
    val studies: List[PracticeConfigStudy]
)

final class PracticeConfigStudy(
    val id: String, // study ID
    val name: String,
    val desc: String
)
