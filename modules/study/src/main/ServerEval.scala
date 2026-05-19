package lila.study

import chess.format.pgn.Glyphs
import chess.format.{ Fen, Uci, UciPath }
import play.api.libs.json.*

import lila.core.perm.Granter
import lila.core.relay.GetCrowd
import lila.db.dsl.bsonWriteOpt
import lila.tree.Node.Comment
import lila.tree.{ Advice, Analysis, Branch, Info, Node, Root }

object ServerEval:

  final class Requester(
      chapterRepo: ChapterRepo,
      userApi: lila.core.user.UserApi
  )(using Executor):

    private val onceEvery = scalalib.cache.OnceEvery[StudyChapterId](5.minutes)

    def apply(study: Study, chapter: Chapter, userId: UserId, official: Boolean = false): Funit =
      chapter.serverEval
        .forall: eval =>
          !eval.done && onceEvery(chapter.id)
        .so:
          for
            isOfficial <- fuccess(official) >>|
              fuccess(userId.is(UserId.lichess)) >>|
              userApi.me(userId).map(_.soUse(Granter.opt(_.Relay)))
            _ <- chapterRepo.startServerEval(chapter)
          yield lila.common.Bus.pub(
            lila.core.fishnet.Bus.StudyChapterRequest(
              studyId = study.id,
              chapterId = chapter.id,
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
              userId = userId,
              official = isOfficial
            )
          )

  final class Merger(
      sequencer: StudySequencer,
      socket: StudySocket,
      chapterRepo: ChapterRepo,
      divider: lila.core.game.Divider,
      analysisJson: lila.tree.AnalysisJson
  )(using Executor, Scheduler):

    def apply(analysis: Analysis, complete: Boolean): Funit = analysis.id match
      case Analysis.Id.Study(studyId, chapterId) =>
        sequencer.sequenceStudyWithChapter(studyId, chapterId):
          case Study.WithChapter(_, chapter) =>
            for
              _ <- complete.so(chapterRepo.completeServerEval(chapter))
              _ <- chapter.root.mainline
                .zip(analysis.infoAdvices)
                .foldM(UciPath.root):
                  case (path, (node, (info, advOpt))) =>
                    saveAnalysis(chapter, node, path, info, advOpt)
                .andDo(sendProgress(studyId, chapterId, analysis))
                .logFailure(logger)
            yield ()
      case _ => funit

    private def saveAnalysis(
        chapter: Chapter,
        node: Branch,
        path: UciPath,
        info: Info,
        advOpt: Option[Advice]
    ): Future[UciPath] =

      val nextPath = path + node.id

      def saveAnalysisLine() =
        chapter.root
          .nodeAt(path)
          .flatMap: parent =>
            analysisLine(parent, chapter.setup.variant, info).map: subTree =>
              parent.addChild(subTree) -> subTree
          .so: (_, subTree) =>
            chapterRepo.addSubTree(chapter, subTree, path, none)

      def saveInfoAdvice() =
        import BSONHandlers.given
        import lila.db.dsl.given
        import lila.study.Node.BsonFields as F
        ((info.eval.score.isDefined && node.eval.isEmpty) || (advOpt.isDefined && !node.comments.hasLichessComment))
          .so(
            chapterRepo
              .setNodeValues(
                chapter,
                nextPath,
                List(
                  F.score -> info.eval.score
                    .ifTrue:
                      node.eval.isEmpty ||
                      advOpt.isDefined && node.comments.findBy(Comment.Author.Lichess).isEmpty
                    .flatMap(bsonWriteOpt),
                  F.comments -> advOpt
                    .map: adv =>
                      node.comments + Comment(
                        Comment.Id.make,
                        adv.makeComment(false),
                        Comment.Author.Lichess
                      )
                    .flatMap(bsonWriteOpt),
                  F.glyphs -> advOpt
                    .map(adv => node.glyphs.merge(Glyphs.fromList(List(adv.judgment.glyph))))
                    .flatMap(bsonWriteOpt)
                )
              )
          )

      saveAnalysisLine()
        >> saveInfoAdvice().inject(nextPath)

    end saveAnalysis

    private def analysisLine(root: Node, variant: chess.variant.Variant, info: Info): Option[Branch] =
      val setup = chess.Position.AndFullMoveNumber(variant, root.fen)
      val (result, error) = setup.position
        .foldRight(info.variation.take(20), setup.ply)(
          none[Branch],
          (step, acc) =>
            inline def branch = makeBranch(step.move, step.ply)
            acc.fold(branch)(acc => branch.addChild(acc)).some
        )
      error.foreach(e => logger.info(e.value))
      result

    private def makeBranch(m: chess.MoveOrDrop, ply: chess.Ply): Branch =
      Branch(
        ply = ply,
        move = Uci.WithSan(m.toUci, m.toSanStr),
        fen = Fen.write(m.after, ply.fullMoveNumber),
        crazyData = m.after.position.crazyData,
        clock = none,
        forceVariation = false
      )

    private def sendProgress(
        studyId: StudyId,
        chapterId: StudyChapterId,
        analysis: Analysis
    ): Funit =
      chapterRepo
        .byId(chapterId)
        .flatMapz: chapter =>
          reallySendToChapter(studyId, chapter).mapz:
            socket.onServerEval(
              studyId,
              ServerEval.Progress(
                chapterId = chapter.id,
                tree = chapter.root,
                analysis = analysisJson.bothPlayers(chapter.root.ply, analysis),
                division = divisionOf(chapter)
              )
            )

    private def reallySendToChapter(studyId: StudyId, chapter: Chapter): Fu[Boolean] =
      if chapter.relay.isEmpty
      then fuTrue
      else
        lila.common.Bus
          .ask[Int, GetCrowd](GetCrowd(studyId, _))
          .map(_ < 1000)

    def divisionOf(chapter: Chapter) =
      divider(
        id = chapter.id.into(GameId),
        sans = chapter.root.mainline.map(_.move.san).toVector,
        variant = chapter.setup.variant,
        initialFen = chapter.root.fen.some
      )

  case class Progress(chapterId: StudyChapterId, tree: Root, analysis: JsObject, division: chess.Division)
