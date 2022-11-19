package lila.study

import alleycats.Zero
import scala.concurrent.duration.*

import lila.hub.AsyncActorSequencers

final private class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mode: play.api.Mode
):

  private val workQueue =
    new AsyncActorSequencers(maxSize = 64, expiration = 1 minute, timeout = 10 seconds, name = "study")

  def sequenceStudy[A <: Matchable: Zero](studyId: Study.Id)(f: Study => Fu[A]): Fu[A] =
    workQueue(studyId) {
      studyRepo.byId(studyId) flatMap {
        _ ?? { f(_) }
      }
    }

  def sequenceStudyWithChapter[A <: Matchable: Zero](studyId: Study.Id, chapterId: Chapter.Id)(
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
