package lila.practice

import lila.study.Study

case class PracticeConfig(
    sections: List[PracticeConfigSection]
) {

  def studyIds = sections.flatMap(_.studies.map(_.id)) map Study.Id.apply
}

object PracticeConfig {
  val empty = PracticeConfig(Nil)
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
