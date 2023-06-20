package lila.study

import chess.format.pgn.Glyphs
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import play.api.libs.json.*

import lila.analyse.{ Analysis, Info }
import lila.hub.actorApi.fishnet.StudyChapterRequest
import lila.security.Granter
import lila.tree.Node.Comment
import lila.user.{ User, UserRepo }
import lila.tree.{ Node, Root, Branch }
import lila.db.dsl.bsonWriteOpt

object ServerEval:

  final class Requester(
      fishnet: lila.hub.actors.Fishnet,
      chapterRepo: ChapterRepo,
      userRepo: UserRepo
  )(using Executor):

    private val onceEvery = lila.memo.OnceEvery[StudyChapterId](5 minutes)

    def apply(study: Study, chapter: Chapter, userId: UserId, unlimited: Boolean = false): Funit =
      chapter.serverEval.fold(true) { eval =>
        !eval.done && onceEvery(chapter.id)
      } so {
        val unlimitedFu =
          fuccess(unlimited) >>|
            fuccess(userId == User.lichessId) >>| userRepo.me(userId).map(Granter.opt(_.Relay)(using _))
        unlimitedFu flatMap { unlimited =>
          chapterRepo.startServerEval(chapter) >>- {
            fishnet ! StudyChapterRequest(
              studyId = study.id,
              chapterId = chapter.id,
              initialFen = chapter.root.fen.some,
              variant = chapter.setup.variant,
              moves = chess.format
                .UciDump(
                  moves = chapter.root.mainline.map(_.move.san),
                  initialFen = chapter.root.fen.some,
                  variant = chapter.setup.variant,
                  force960Notation = true
                )
                .toOption
                .map(_.flatMap(chess.format.Uci.apply)) | List.empty,
              userId = userId,
              unlimited = unlimited
            )
          }
        }
      }

  final class Merger(
      sequencer: StudySequencer,
      socket: StudySocket,
      chapterRepo: ChapterRepo,
      divider: lila.game.Divider
  )(using Executor):

    def apply(analysis: Analysis, complete: Boolean): Funit =
      analysis.studyId so { studyId =>
        sequencer.sequenceStudyWithChapter(studyId, analysis.id into StudyChapterId) {
          case Study.WithChapter(_, chapter) =>
            (complete so chapterRepo.completeServerEval(chapter)) >> {
              lila.common.LilaFuture
                .fold(chapter.root.mainline.zip(analysis.infoAdvices).toList)(UciPath.root) {
                  case (path, (node, (info, advOpt))) =>
                    chapter.root
                      .nodeAt(path)
                      .flatMap: parent =>
                        analysisLine(parent, chapter.setup.variant, info).map: subTree =>
                          parent.addChild(subTree) -> subTree
                      .so { (newParent, subTree) =>
                        chapterRepo.addSubTree(subTree, newParent, path)(chapter)
                      } >> {
                      import BSONHandlers.given
                      import lila.db.dsl.given
                      import lila.study.Node.{ BsonFields as F }
                      ((info.eval.score.isDefined && node.eval.isEmpty) || (advOpt.isDefined && !node.comments.hasLichessComment)) so
                        chapterRepo
                          .setNodeValues(
                            chapter,
                            path + node.id,
                            List(
                              F.score -> info.eval.score
                                .ifTrue {
                                  node.eval.isEmpty ||
                                  advOpt.isDefined && node.comments.findBy(Comment.Author.Lichess).isEmpty
                                }
                                .flatMap(bsonWriteOpt),
                              F.comments -> advOpt
                                .map { adv =>
                                  node.comments + Comment(
                                    Comment.Id.make,
                                    adv.makeComment(withEval = false, withBestMove = true) into Comment.Text,
                                    Comment.Author.Lichess
                                  )
                                }
                                .flatMap(bsonWriteOpt),
                              F.glyphs -> advOpt
                                .map { adv =>
                                  node.glyphs merge Glyphs.fromList(List(adv.judgment.glyph))
                                }
                                .flatMap(bsonWriteOpt)
                            )
                          )
                    } inject path + node.id
                } void
            } >>- {
              chapterRepo
                .byId(analysis.id into StudyChapterId)
                .foreach:
                  _ so { chapter =>
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
            } logFailure logger
        }
      }

    def divisionOf(chapter: Chapter) =
      divider(
        id = chapter.id into GameId,
        sans = chapter.root.mainline.map(_.move.san).toVector,
        variant = chapter.setup.variant,
        initialFen = chapter.root.fen.some
      )

    private def analysisLine(root: Node, variant: chess.variant.Variant, info: Info): Option[Branch] =
      val (_, games, error) = chess.Replay.gameMoveWhileValid(info.variation take 20, root.fen, variant)
      error foreach { e => logger.info(e.value) }
      games.reverse match
        case Nil => none
        case (g, m) :: rest =>
          rest
            .foldLeft[Branch](makeBranch(g, m)) { case (node, (g, m)) =>
              makeBranch(g, m) addChild node
            } some

    private def makeBranch(g: chess.Game, m: Uci.WithSan) =
      Branch(
        id = UciCharPair(m.uci),
        ply = g.ply,
        move = m,
        fen = Fen write g,
        check = g.situation.check,
        crazyData = g.situation.board.crazyData,
        clock = none,
        forceVariation = false
      )

  case class Progress(chapterId: StudyChapterId, tree: Root, analysis: JsObject, division: chess.Division)

  def toJson(chapter: Chapter, analysis: Analysis) =
    lila.analyse.JsonView.bothPlayers(chapter.root.ply, analysis)
