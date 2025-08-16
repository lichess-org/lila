package lila.practice

import lila.core.study.data.StudyName
import lila.study.Chapter

case class PracticeStructure(sections: List[PracticeSection]):

  def study(id: StudyId): Option[PracticeStudy] =
    sections.flatMap(_.study(id)).headOption

  lazy val studiesByIds: Map[StudyId, PracticeStudy] =
    sections.view
      .flatMap(_.studies)
      .mapBy(_.id)

  lazy val sectionsByStudyIds: Map[StudyId, PracticeSection] =
    sections.view.flatMap { sec =>
      sec.studies.map { stu =>
        stu.id -> sec
      }
    }.toMap

  lazy val chapterIds: List[StudyChapterId] = sections.flatMap(_.studies).flatMap(_.chapterIds)

  lazy val nbChapters = sections.flatMap(_.studies).map(_.chapterIds.size).sum

  def findSection(id: StudyId): Option[PracticeSection] = sectionsByStudyIds.get(id)

case class PracticeSection(
    id: String,
    name: String,
    studies: List[PracticeStudy]
):
  lazy val studiesByIds: Map[StudyId, PracticeStudy] = studies.mapBy(_.id)

  def study(id: StudyId): Option[PracticeStudy] = studiesByIds.get(id)

case class PracticeStudy(
    id: StudyId,
    name: StudyName,
    desc: String,
    chapters: List[Chapter.IdName]
) extends lila.core.practice.Study:
  val slug = scalalib.StringOps.slug(name.value)
  val chapterIds = chapters.map(_.id)

object PracticeStructure:

  private[practice] val totalChapters = 233

  private[practice] def studyIds: List[StudyId] = PracticeSections.list.flatMap(_.studies.map(_.id))

  def withChapters(chapters: Map[StudyId, Vector[Chapter.IdName]]) = PracticeStructure:
    PracticeSections.list.map: sec =>
      sec.copy(
        studies = sec.studies.map: stu =>
          stu.copy(chapters = chapters.get(stu.id).so(_.toList))
      )
