package lila.study

import play.api.libs.json._

import lila.analyse.Analysis
import lila.hub.actorApi.fishnet.StudyChapterRequest
import lila.hub.actorApi.map.Tell
import lila.tree._
import lila.tree.Node.Comment
import lila.user.User

object ServerEval {

  final class Requester(
      fishnetActor: akka.actor.ActorSelection,
      chapterRepo: ChapterRepo
  ) {

    def apply(study: Study, chapter: Chapter, userId: User.ID): Funit =
      chapterRepo.setAnalysed(chapter.id, false.some) >>- {
        fishnetActor ! StudyChapterRequest(
          studyId = study.id.value,
          chapterId = chapter.id.value,
          initialFen = chapter.root.fen.some,
          variant = chapter.setup.variant,
          moves = chess.format.UciDump(
            moves = chapter.root.mainline.map(_.move.san),
            initialFen = chapter.root.fen.value.some,
            variant = chapter.setup.variant
          ).toOption.map(_.map(chess.format.Uci.apply).flatten) | List.empty,
          userId = userId.some
        )
      }
  }

  final class Merger(
      sequencer: StudySequencer,
      socketHub: akka.actor.ActorRef,
      api: StudyApi,
      chapterRepo: ChapterRepo
  ) {

    def apply(analysis: Analysis, complete: Boolean): Funit = analysis.studyId ?? { studyId =>
      sequencer.sequenceStudyWithChapter(Study.Id(studyId), Chapter.Id(analysis.id)) {
        case Study.WithChapter(study, chapter) =>
          (complete ?? chapterRepo.setAnalysed(chapter.id, true.some)) >> {
            val allInfoAdvices = analysis.infos.headOption.map(_ -> none).toList ::: analysis.infoAdvices
            lila.common.Future.fold(chapter.root.mainline zip allInfoAdvices)(Path.root) {
              case (path, (node, (info, advOpt))) => info.eval.score.ifTrue(true || node.score.isEmpty).?? { score =>
                chapterRepo.setScore(chapter, path, score.some) >>
                  advOpt.?? { adv =>
                    chapterRepo.setComments(chapter, path, node.comments + Comment(
                      Comment.Id.make,
                      Comment.Text(adv.makeComment(false, true)),
                      Comment.Author.Lichess
                    ))
                  }
              } inject path + node
            } void
          } >>- {
            socketHub ! Tell(study.id.value, Socket.UpdateChapter(lila.socket.Socket.Uid(""), chapter.id))
          }
        // socketHub ! Tell(studyId, ServerEval.Progress(
        //   chapterId = chapter.id,
        //   tree = lila.round.TreeBuilder(
        //     id = analysis.id,
        //     pgnMoves = chapter.root.mainline.map(_.move.san)(scala.collection.breakOut),
        //     variant = chapter.setup.variant,
        //     analysis = analysis.some,
        //     initialFen = chapter.root.fen,
        //     withFlags = lila.round.JsonView.WithFlags(),
        //     clocks = none
        //   ),
        //   analysis = ServerEval.toJson(chapter, analysis)
        // ))
      }
    }
  }

  case class Progress(chapterId: Chapter.Id, tree: Root, analysis: JsObject)

  def toJson(chapter: Chapter, analysis: Analysis) =
    lila.analyse.JsonView.bothPlayers(
      lila.analyse.Accuracy.PovLike(chess.White, chapter.root.color, chapter.root.ply),
      analysis
    )
}
