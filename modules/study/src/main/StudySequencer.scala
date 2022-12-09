package lila.study

import alleycats.Zero
import scala.concurrent.duration.*

import lila.hub.AsyncActorSequencers
import lila.common.config.Max

final private class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using scala.concurrent.ExecutionContext, akka.actor.Scheduler, play.api.Mode):

  private val workQueue = AsyncActorSequencers[StudyId](
    maxSize = Max(64),
    expiration = 1 minute,
    timeout = 10 seconds,
    name = "study"
  )

  def sequenceStudy[A <: Matchable: Zero](studyId: StudyId)(f: Study => Fu[A]): Fu[A] =
    workQueue(studyId) {
      studyRepo.byId(studyId) flatMap {
        _ ?? { f(_) }
      }
    }

  def sequenceStudyWithChapter[A <: Matchable: Zero](studyId: StudyId, chapterId: StudyChapterId)(
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
