package lila.study

import play.api.libs.json._

import chess.format.pgn.Glyphs
import chess.format.{ Forsyth, FEN, Uci, UciCharPair }
import lila.analyse.{ Analysis, Info }
import lila.hub.actorApi.fishnet.StudyChapterRequest
import lila.hub.actorApi.map.Tell
import lila.socket.Socket.Uid
import lila.tree._
import lila.tree.Node.Comment
import lila.user.User

object ServerEval {

  final class Requester(
      fishnetActor: akka.actor.ActorSelection,
      chapterRepo: ChapterRepo
  ) {

    def apply(study: Study, chapter: Chapter, userId: User.ID): Funit = chapter.serverEval.isEmpty ?? {
      chapterRepo.startServerEval(chapter) >>- {
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
          userId = userId
        )
      }
    }
  }

  final class Merger(
      sequencer: StudySequencer,
      socketMap: SocketMap,
      api: StudyApi,
      chapterRepo: ChapterRepo,
      divider: lila.game.Divider
  ) {

    def apply(analysis: Analysis, complete: Boolean): Funit = analysis.studyId ?? { studyId =>
      sequencer.sequenceStudyWithChapter(Study.Id(studyId), Chapter.Id(analysis.id)) {
        case Study.WithChapter(study, chapter) =>
          (complete ?? chapterRepo.completeServerEval(chapter)) >> {
            lila.common.Future.fold(chapter.root.mainline zip analysis.infoAdvices)(Path.root) {
              case (path, (node, (info, advOpt))) => info.eval.score.ifTrue(node.score.isEmpty).?? { score =>
                chapterRepo.setScore(chapter, path + node, score.some) >>
                  advOpt.?? { adv =>
                    chapterRepo.setComments(chapter, path + node, node.comments + Comment(
                      Comment.Id.make,
                      Comment.Text(adv.makeComment(false, true)),
                      Comment.Author.Lichess
                    )) >>
                      chapterRepo.setGlyphs(
                        chapter,
                        path + node,
                        node.glyphs merge Glyphs.fromList(List(adv.judgment.glyph))
                      ) >> {
                          chapter.root.nodeAt(path).flatMap { parent =>
                            analysisLine(parent, chapter.setup.variant, info) flatMap { child =>
                              parent.addChild(child).children.get(child.id)
                            }
                          } ?? { chapterRepo.setChild(chapter, path, _) }
                        }
                  }
              } inject path + node
            } void
          } >>- {
            chapterRepo.byId(Chapter.Id(analysis.id)).foreach {
              _ ?? { chapter =>
                socketMap.tell(studyId, ServerEval.Progress(
                  chapterId = chapter.id,
                  tree = lila.study.TreeBuilder(chapter.root, chapter.setup.variant),
                  analysis = toJson(chapter, analysis),
                  division = divisionOf(chapter)
                ))
              }
            }
          } logFailure logger
      }
    }

    def divisionOf(chapter: Chapter) = divider(
      id = chapter.id.value,
      pgnMoves = chapter.root.mainline.map(_.move.san).toVector,
      variant = chapter.setup.variant,
      initialFen = chapter.root.fen.some
    )

    private def analysisLine(root: RootOrNode, variant: chess.variant.Variant, info: Info): Option[Node] =
      chess.Replay.gameMoveWhileValid(info.variation take 20, root.fen.value, variant) match {
        case (init, games, error) =>
          error foreach { logger.info(_) }
          games.reverse match {
            case Nil => none
            case (g, m) :: rest => rest.foldLeft(makeBranch(g, m)) {
              case (node, (g, m)) => makeBranch(g, m) addChild node
            } some
          }
      }

    private def makeBranch(g: chess.Game, m: Uci.WithSan) = {
      val fen = FEN(Forsyth >> g)
      Node(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = fen,
        check = g.situation.check,
        crazyData = g.situation.board.crazyData,
        clock = none,
        children = Node.emptyChildren,
        forceVariation = false
      )
    }
  }

  case class Progress(chapterId: Chapter.Id, tree: Root, analysis: JsObject, division: chess.Division)

  def toJson(chapter: Chapter, analysis: Analysis) =
    lila.analyse.JsonView.bothPlayers(
      lila.analyse.Accuracy.PovLike(chess.White, chapter.root.color, chapter.root.ply),
      analysis
    )
}
