package lila.study

import lila.hub.{ Duct, DuctMap }
import lila.hub.actorApi.map.Tell

private final class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencers: DuctMap[_]
) {

  def sequenceStudy(studyId: Study.Id)(f: Study => Funit): Funit =
    sequence(studyId) {
      studyRepo.byId(studyId) flatMap {
        _ ?? { f(_) }
      }
    }

  def sequenceStudyWithChapter(studyId: Study.Id, chapterId: Chapter.Id)(f: Study.WithChapter => Funit): Funit =
    sequenceStudy(studyId) { study =>
      chapterRepo.byId(chapterId) flatMap {
        _ ?? { chapter =>
          f(Study.WithChapter(study, chapter))
        }
      }
    }

  private def sequence(studyId: Study.Id)(f: => Funit): Funit = {
    val promise = scala.concurrent.Promise[Unit]
    sequencers.tell(studyId.value, Duct.extra.LazyPromise(Duct.extra.LazyFu(() => f), promise))
    promise.future
  }
}
