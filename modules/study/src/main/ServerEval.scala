package lila.study

import chess.format.pgn.Glyphs
import chess.format.{ Forsyth, Uci, UciCharPair }
import play.api.libs.json._
import scala.concurrent.duration._

import lila.analyse.{ Analysis, Info }
import lila.hub.actorApi.fishnet.StudyChapterRequest
import lila.tree.Node.Comment
import lila.user.User
import lila.{ tree => T }

object ServerEval {

  final class Requester(
      fishnet: lila.hub.actors.Fishnet,
      chapterRepo: ChapterRepo
  )(implicit ec: scala.concurrent.ExecutionContext) {

    private val onceEvery = lila.memo.OnceEvery(5 minutes)

    def apply(study: Study, chapter: Chapter, userId: User.ID): Funit =
      chapter.serverEval.fold(true) { eval =>
        !eval.done && onceEvery(chapter.id.value)
      } ?? {
        chapterRepo.startServerEval(chapter) >>- {
          fishnet ! StudyChapterRequest(
            studyId = study.id.value,
            chapterId = chapter.id.value,
            initialFen = chapter.root.fen.some,
            variant = chapter.setup.variant,
            moves = chess.format
              .UciDump(
                moves = chapter.root.mainline.map(_.move.san),
                initialFen = chapter.root.fen.some,
                variant = chapter.setup.variant
              )
              .toOption
              .map(_.flatMap(chess.format.Uci.apply)) | List.empty,
            userId = userId
          )
        }
      }
  }

  final class Merger(
      sequencer: StudySequencer,
      socket: StudySocket,
      chapterRepo: ChapterRepo,
      divider: lila.game.Divider
  )(implicit ec: scala.concurrent.ExecutionContext) {

    def apply(analysis: Analysis, complete: Boolean): Funit =
      analysis.studyId.map(Study.Id.apply) ?? { studyId =>
        sequencer.sequenceStudyWithChapter(studyId, Chapter.Id(analysis.id)) {
          case Study.WithChapter(_, chapter) =>
            (complete ?? chapterRepo.completeServerEval(chapter)) >> {
              lila.common.Future
                .fold(chapter.root.mainline.zip(analysis.infoAdvices).toList)(Path.root) {
                  case (path, (node, (info, advOpt))) =>
                    info.eval.score
                      .ifTrue {
                        node.score.isEmpty ||
                        advOpt.isDefined && node.comments.findBy(Comment.Author.Lichess).isEmpty
                      }
                      .?? { score =>
                        chapterRepo.setScore(score.some)(chapter, path + node) >>
                          advOpt.?? { adv =>
                            chapterRepo.setComments(
                              node.comments + Comment(
                                Comment.Id.make,
                                Comment.Text(adv.makeComment(withEval = false, withBestMove = true)),
                                Comment.Author.Lichess
                              )
                            )(chapter, path + node) >>
                              chapterRepo.setGlyphs(
                                node.glyphs merge Glyphs.fromList(List(adv.judgment.glyph))
                              )(chapter, path + node) >> {
                                chapter.root.nodeAt(path).flatMap { parent =>
                                  analysisLine(parent, chapter.setup.variant, info) map parent.addChild
                                } ?? { parentWithNewChildren =>
                                  chapterRepo.setChildren(parentWithNewChildren.children)(chapter, path)
                                }
                              }
                          }
                      } inject path + node
                } void
            } >>- {
              chapterRepo.byId(Chapter.Id(analysis.id)).foreach {
                _ ?? { chapter =>
                  socket.onServerEval(
                    studyId,
                    ServerEval.Progress(
                      chapterId = chapter.id,
                      tree = lila.study.TreeBuilder(chapter.root, chapter.setup.variant),
                      analysis = toJson(chapter, analysis),
                      division = divisionOf(chapter)
                    )
                  )
                }
              }
            } logFailure logger
        }
      }

    def divisionOf(chapter: Chapter) =
      divider(
        id = chapter.id.value,
        pgnMoves = chapter.root.mainline.map(_.move.san).toVector,
        variant = chapter.setup.variant,
        initialFen = chapter.root.fen.some
      )

    private def analysisLine(root: RootOrNode, variant: chess.variant.Variant, info: Info): Option[Node] =
      chess.Replay.gameMoveWhileValid(info.variation take 20, root.fen, variant) match {
        case (_, games, error) =>
          error foreach { logger.info(_) }
          games.reverse match {
            case Nil => none
            case (g, m) :: rest =>
              rest
                .foldLeft(makeBranch(g, m)) { case (node, (g, m)) =>
                  makeBranch(g, m) addChild node
                } some
          }
      }

    private def makeBranch(g: chess.Game, m: Uci.WithSan) =
      Node(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = Forsyth >> g,
        check = g.situation.check,
        crazyData = g.situation.board.crazyData,
        clock = none,
        children = Node.emptyChildren,
        forceVariation = false
      )
  }

  case class Progress(chapterId: Chapter.Id, tree: T.Root, analysis: JsObject, division: chess.Division)

  def toJson(chapter: Chapter, analysis: Analysis) =
    lila.analyse.JsonView.bothPlayers(
      lila.analyse.Accuracy.PovLike(chess.White, chapter.root.color, chapter.root.ply),
      analysis
    )
}
