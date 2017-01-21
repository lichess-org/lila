package lila.practice

import lila.study.{ Study, Chapter }

case class PracticeStructure(
  sections: List[PracticeSection])

object PracticeStructure {
  def make(conf: PracticeConfig, chapters: Map[Study.Id, Vector[Chapter.IdName]]) = PracticeStructure(
    sections = conf.sections.map { sec =>
      PracticeSection(
        id = sec.id,
        name = sec.name,
        studies = sec.studies.map { stu =>
          val id = Study.Id(stu.id)
          PracticeStudy(
            id = id,
            name = stu.name,
            desc = stu.name,
            chapters = chapters.get(id).??(_.toList))
        })
    })
}

case class PracticeSection(
  id: String,
  name: String,
  studies: List[PracticeStudy])

case class PracticeStudy(
  id: Study.Id, // study ID
  name: String,
  desc: String,
  chapters: List[Chapter.IdName])
