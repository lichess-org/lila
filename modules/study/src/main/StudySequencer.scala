package lila.study

import scala.concurrent.duration._

import lila.common.WorkQueues

private final class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(implicit mat: akka.stream.Materializer) {

  private val workQueue = new WorkQueues(256, 10 minutes)

  def sequenceStudy(studyId: Study.Id)(f: Study => Funit): Funit =
    workQueue(studyId.value) {
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
}
