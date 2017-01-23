package lila.practice

import lila.study.{ Study, Chapter }

case class PracticeStructure(
    sections: List[PracticeSection]) {

  def study(id: Study.Id): Option[PracticeStudy] =
    sections.flatMap(_ study id).headOption

  def prev(id: Study.Id): Option[PracticeStudy] =
    studyPosition(id).map(_ + 1) flatMap allStudies.lift
  def next(id: Study.Id): Option[PracticeStudy] =
    studyPosition(id).map(_ - 1) flatMap allStudies.lift

  private def studyPosition(id: Study.Id): Option[Int] =
    sections.indexOf(id).some.filter(0 <=)

  lazy val allStudies: Vector[PracticeStudy] =
    sections.flatMap(_.studies).toVector

  def hasStudy(id: Study.Id) = allStudies.exists(_.id == id)
}

case class PracticeSection(
    id: String,
    name: String,
    studies: List[PracticeStudy]) {

  def study(id: Study.Id): Option[PracticeStudy] = studies find (_.id == id)
}

case class PracticeStudy(
    id: Study.Id, // study ID
    name: String,
    desc: String,
    chapters: List[Chapter.IdName]) {

  val slug = lila.common.String slugify name

  def chapterIds = chapters.map(_.id)
}

object PracticeStructure {

  def make(conf: PracticeConfig, chapters: Map[Study.Id, Vector[Chapter.IdName]]) =
    PracticeStructure(
      sections = conf.sections.map { sec =>
        PracticeSection(
          id = sec.id,
          name = sec.name,
          studies = sec.studies.map { stu =>
            val id = Study.Id(stu.id)
            PracticeStudy(
              id = id,
              name = stu.name,
              desc = stu.desc,
              chapters = chapters.get(id).??(_.toList))
          })
      })
}
