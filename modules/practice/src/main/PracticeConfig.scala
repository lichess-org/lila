package lila.practice

import io.methvin.play.autoconfig._
import lila.study.Study
import lila.common.config._

case class PracticeConfig(
    sections: List[PracticeConfigSection]
) {

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id.apply
}

object PracticeConfig {
  val empty = PracticeConfig(Nil)

  private implicit val studyLoader = AutoConfig.loader[PracticeConfigStudy]
  private implicit val sectionLoader = AutoConfig.loader[PracticeConfigSection]
  implicit val loader = AutoConfig.loader[PracticeConfig]
}

case class PracticeConfigSection(
    id: String,
    name: String,
    studies: List[PracticeConfigStudy]
)

case class PracticeConfigStudy(
    id: String, // study ID
    name: String,
    desc: String
)
