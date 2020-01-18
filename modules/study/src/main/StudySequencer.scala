package lila.study

import scala.concurrent.duration._

import lila.common.WorkQueues

final private class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(
    implicit ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
) {

  private val workQueue =
    new WorkQueues(buffer = 256, expiration = 5 minutes, timeout = 10 seconds, name = "study")

  def sequenceStudy(studyId: Study.Id)(f: Study => Funit): Funit =
    workQueue(studyId.value) {
      studyRepo.byId(studyId) flatMap {
        _ ?? { f(_) }
      }
    }

  def sequenceStudyWithChapter(studyId: Study.Id, chapterId: Chapter.Id)(
      f: Study.WithChapter => Funit
  ): Funit =
    sequenceStudy(studyId) { study =>
      chapterRepo.byId(chapterId) flatMap {
        _ ?? { chapter =>
          f(Study.WithChapter(study, chapter))
        }
      }
    }
}
