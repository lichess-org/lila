package lidraughts.study

import play.api.libs.json._

import draughts.format.{ Forsyth, FEN, Uci, UciCharPair }
import draughts.format.pdn.Glyphs
import lidraughts.analyse.{ Analysis, Info }
import lidraughts.hub.actorApi.fishnet.StudyChapterRequest
import lidraughts.hub.actorApi.map.Tell
import lidraughts.socket.Socket.Uid
import lidraughts.tree._
import lidraughts.tree.Node.Comment
import lidraughts.user.User

object ServerEval {

  final class Requester(
      fishnetActor: akka.actor.ActorSelection,
      chapterRepo: ChapterRepo
  ) {

    def apply(study: Study, chapter: Chapter, userId: User.ID): Funit =
      chapterRepo.startServerEval(chapter) >>- {
        fishnetActor ! StudyChapterRequest(
          studyId = study.id.value,
          chapterId = chapter.id.value,
          initialFen = chapter.root.fen.some,
          variant = chapter.setup.variant,
          moves = draughts.format.UciDump(
            moves = chapter.root.mainline.map(_.move.san),
            initialFen = chapter.root.fen.value.some,
            variant = chapter.setup.variant
          ).toOption.map(_.map(draughts.format.Uci.apply).flatten) | List.empty,
          userId = userId.some
        )
      }
  }

  final class Merger(
      sequencer: StudySequencer,
      socketHub: akka.actor.ActorRef,
      api: StudyApi,
      chapterRepo: ChapterRepo,
      divider: lidraughts.game.Divider
  ) {

    def apply(analysis: Analysis, complete: Boolean): Funit = analysis.studyId ?? { studyId =>
      sequencer.sequenceStudyWithChapter(Study.Id(studyId), Chapter.Id(analysis.id)) {
        case Study.WithChapter(study, chapter) =>
          (complete ?? chapterRepo.completeServerEval(chapter)) >> {
            lidraughts.common.Future.fold(chapter.root.mainline zip analysis.infoAdvices)(Path.root) {
              case (path, (node, (info, advOpt))) => info.eval.score.ifTrue(node.score.isEmpty).?? { score =>
                chapterRepo.setScore(chapter, path + node, score.some) >>
                  advOpt.?? { adv =>
                    chapterRepo.setComments(chapter, path + node, node.comments + Comment(
                      Comment.Id.make,
                      Comment.Text(adv.makeComment(false, true)),
                      Comment.Author.Lidraughts
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
                socketHub ! Tell(studyId, ServerEval.Progress(
                  chapterId = chapter.id,
                  tree = lidraughts.study.TreeBuilder(chapter.root, chapter.setup.variant),
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
      pdnmoves = chapter.root.mainline.map(_.move.san).toVector,
      variant = chapter.setup.variant,
      initialFen = chapter.root.fen.some
    )

    private def analysisLine(root: RootOrNode, variant: draughts.variant.Variant, info: Info): Option[Node] =
      draughts.Replay.gameMoveWhileValid(info.variation take 20, root.fen.value, variant) match {
        case (init, games, error) =>
          error foreach { logger.info(_) }
          games.reverse match {
            case Nil => none
            case (g, m) :: rest => rest.foldLeft(makeBranch(g, m)) {
              case (node, (g, m)) => makeBranch(g, m) addChild node
            } some
          }
      }

    private def makeBranch(g: draughts.DraughtsGame, m: Uci.WithSan) = {
      val fen = FEN(Forsyth >> g)
      Node(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = fen,
        clock = none,
        children = Node.emptyChildren
      )
    }
  }

  case class Progress(chapterId: Chapter.Id, tree: Root, analysis: JsObject, division: draughts.Division)

  def toJson(chapter: Chapter, analysis: Analysis) =
    lidraughts.analyse.JsonView.bothPlayers(
      lidraughts.analyse.Accuracy.PovLike(draughts.White, chapter.root.color, chapter.root.ply),
      analysis
    )
}
