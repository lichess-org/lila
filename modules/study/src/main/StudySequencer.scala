package lila.study

import lila.hub.Sequencer
import lila.hub.actorApi.map.Tell

private final class StudySequencer(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo,
    sequencers: akka.actor.ActorRef
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
    val work = Sequencer.work(f, promise.some)
    sequencers ! Tell(studyId.value, work)
    promise.future
  }
}
