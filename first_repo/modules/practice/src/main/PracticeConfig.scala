package lila.practice

import io.methvin.play.autoconfig._
import lila.study.Study
import lila.common.config._

final class PracticeConfig(
    val sections: List[PracticeConfigSection]
) {

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id.apply
}

object PracticeConfig {
  val empty = new PracticeConfig(Nil)

  implicit private val studyLoader   = AutoConfig.loader[PracticeConfigStudy]
  implicit private val sectionLoader = AutoConfig.loader[PracticeConfigSection]
  implicit val loader                = AutoConfig.loader[PracticeConfig]
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
