package lila.study

import ornicar.scalalib.Zero
import scala.concurrent.duration._

import lila.hub.AsyncActorSequencers

final private class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode
) {

  private val workQueue =
    new AsyncActorSequencers(maxSize = 64, expiration = 1 minute, timeout = 10 seconds, name = "study")

  def sequenceStudy[A: Zero](studyId: Study.Id)(f: Study => Fu[A]): Fu[A] =
    workQueue(studyId.value) {
      studyRepo.byId(studyId) flatMap {
        _ ?? { f(_) }
      }
    }

  def sequenceStudyWithChapter[A: Zero](studyId: Study.Id, chapterId: Chapter.Id)(
      f: Study.WithChapter => Fu[A]
  ): Fu[A] =
    sequenceStudy(studyId) { study =>
      chapterRepo
        .byId(chapterId)
        .flatMap {
          _.filter(_.studyId == studyId) ?? { chapter =>
            f(Study.WithChapter(study, chapter))
          }
        }
        .mon(_.study.sequencer.chapterTime)
    }
}
