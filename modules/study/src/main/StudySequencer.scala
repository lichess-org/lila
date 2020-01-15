package lila.study

import scala.concurrent.duration._
import java.util.concurrent.TimeoutException

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

  def sequenceStudy(name: String)(studyId: Study.Id)(f: Study => Funit): Funit =
    workQueue(studyId.value) {
      studyRepo.byId(studyId) flatMap {
        _ ?? { f(_) }
      }
    } addFailureEffect logName(name)

  def sequenceStudyWithChapter(name: String)(studyId: Study.Id, chapterId: Chapter.Id)(
      f: Study.WithChapter => Funit
  ): Funit =
    sequenceStudy(name)(studyId) { study =>
      chapterRepo.byId(chapterId) flatMap {
        _ ?? { chapter =>
          f(Study.WithChapter(study, chapter))
        }
      }
    }

  private def logName(name: String): Throwable => Unit = {
    case _: TimeoutException => logger.warn(s"$name timed out")
    case _                   =>
  }
}
