package lila.study

import scala.concurrent.duration._

import lila.hub.DuctSequencers

final private class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  private val workQueue =
    new DuctSequencers(maxSize = 64, expiration = 1 minute, timeout = 10 seconds, name = "study")

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
        _.filter(_.studyId == studyId) ?? { chapter =>
          f(Study.WithChapter(study, chapter))
        }
      }
    }
}
