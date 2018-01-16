package lila.study

import lila.analyse.Analysis
import lila.hub.actorApi.fishnet.StudyChapterRequest
import lila.hub.actorApi.map.Tell
import lila.tree._
import lila.user.User

private final class ServerEval(
    socketHub: akka.actor.ActorRef,
    fishnetActor: akka.actor.ActorSelection,
    chapterRepo: ChapterRepo
) {

  def progress(analysis: Analysis, complete: Boolean): Funit = analysis.studyId ?? { studyId =>
    chapterRepo byId Chapter.Id(analysis.id) flatMap {
      _ ?? { chapter =>
        complete ?? chapterRepo.setAnalysed(chapter.id, true.some) >>- {
          socketHub ! Tell(studyId, ServerEval.Progress(
            chapterId = chapter.id,
            tree = lila.round.TreeBuilder(
              id = analysis.id,
              pgnMoves = chapter.root.mainline.map(_.move.san)(scala.collection.breakOut),
              variant = chapter.setup.variant,
              analysis = analysis.some,
              initialFen = chapter.root.fen,
              withFlags = lila.round.JsonView.WithFlags(),
              clocks = none
            )
          ))
        }
      }
    }
  }

  def request(study: Study, chapter: Chapter, userId: User.ID): Funit =
    chapterRepo.setAnalysed(chapter.id, false.some) >>- {
      fishnetActor ! StudyChapterRequest(
        studyId = study.id.value,
        chapterId = chapter.id.value,
        initialFen = chapter.root.fen.some,
        variant = chapter.setup.variant,
        moves = chapter.root.mainline.map(_.move.uci),
        userId = userId.some
      )
    }
}

private object ServerEval {

  case class Progress(chapterId: Chapter.Id, tree: Root)
}
