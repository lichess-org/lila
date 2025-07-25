package lila.practice

import lila.core.study.data.{ StudyChapterName, StudyName }
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

  lazy val nbUnhiddenChapters =
    sections.filterNot(_.hide).flatMap(_.studies).filterNot(_.hide).map(_.chapterIds.size).sum

  def findSection(id: StudyId): Option[PracticeSection] = sectionsByStudyIds.get(id)

case class PracticeSection(
    id: String,
    hide: Boolean,
    name: String,
    studies: List[PracticeStudy]
):
  lazy val studiesByIds: Map[StudyId, PracticeStudy] = studies.mapBy(_.id)

  def study(id: StudyId): Option[PracticeStudy] = studiesByIds.get(id)

case class PracticeStudy(
    id: StudyId,
    hide: Boolean,
    name: StudyName,
    desc: String,
    chapters: List[Chapter.IdName]
) extends lila.core.practice.Study:
  val slug = scalalib.StringOps.slug(name.value)
  def chapterIds = chapters.map(_.id)

object PracticeStructure:

  val totalChapters = 233

  def isChapterNameCommented(name: StudyChapterName) = name.value.startsWith("//")

  def make(conf: PracticeConfig, chapters: Map[StudyId, Vector[Chapter.IdName]]) =
    PracticeStructure(
      sections = conf.sections.map { sec =>
        PracticeSection(
          id = sec.id,
          hide = ~sec.hide,
          name = sec.name,
          studies = sec.studies.map { stu =>
            val id = StudyId(stu.id)
            PracticeStudy(
              id = id,
              hide = ~stu.hide,
              name = stu.name,
              desc = stu.desc,
              chapters = chapters
                .get(id)
                .so(_.filterNot { c =>
                  isChapterNameCommented(c.name)
                }.toList)
            )
          }
        )
      }
    )
