package lila.study

import chess.format.pgn.Glyphs as BaseGlyphs
import chess.format.{ Fen, Uci, UciPath }
import play.api.libs.json.*

import lila.core.perm.Granter
import lila.core.relay.GetCrowd
import lila.db.dsl.bsonWriteOpt
import lila.tree.Node.{ Comment, Glyphs as NodeGlyphs }
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
              _ <- chapterRepo.removeAnalysisGameId(chapter.id)
              _ <- complete.so(chapterRepo.completeServerEval(chapter))
              _ <- chapter.root.mainline
                .zip(analysis.infoAdvices)
                .foldM(chapter.root -> UciPath.root):
                  case ((root, path), (node, (info, advOpt))) =>
                    saveAnalysis(chapter, root, node, path, info, advOpt)
                .void
              _ <- sendProgress(studyId, chapterId, analysis).logFailure(logger)
            yield ()
      case _ => funit

    private def saveAnalysis(
        chapter: Chapter,
        root: Root,
        node: Branch,
        path: UciPath,
        info: Info,
        advOpt: Option[Advice]
    ): Future[(Root, UciPath)] =

      val nextPath = path + node.id

      def saveAnalysisLine(): Fu[Root] =
        val merged = root
          .nodeAt(path)
          .flatMap: parent =>
            analysisLine(parent, chapter.setup.variant, info).map: line =>
              val subTree = line
                .copy(children = line.children.updateAllWith(_.setComp))
                .setComp
              val updatedRoot = mergeAnalysis(root, path, subTree)
              updatedRoot -> updatedRoot.nodeAt(path).flatMap(_.children.get(subTree.id))
        merged match
          case Some((updatedRoot, Some(subTree))) =>
            chapterRepo.addSubTree(chapter, subTree, path, none).inject(updatedRoot)
          case Some((updatedRoot, None)) => fuccess(updatedRoot)
          case None => fuccess(root)

      def saveInfoAdvice() =
        import BSONHandlers.given
        import lila.db.dsl.given
        import lila.study.Node.BsonFields as F
        val saveScore = info.eval.score.isDefined && node.eval.isEmpty
        val saveAdvice = advOpt.isDefined && !node.comments.hasLichessComment
        (saveScore || saveAdvice)
          .so(
            chapterRepo
              .setNodeValues(
                chapter,
                nextPath,
                List(
                  F.score -> BSONHandlers
                    .writeEval(info.eval.copy(static = true).some)
                    .filter(_ => saveScore),
                  F.comments -> advOpt
                    .map: adv =>
                      node.comments + Comment(
                        Comment.Id.make,
                        adv.makeComment(false),
                        Comment.Author.Lichess,
                        comp = true
                      )
                    .flatMap(bsonWriteOpt),
                  F.glyphs -> advOpt
                    .map: adv =>
                      node.glyphs.merge(
                        NodeGlyphs.fromBase(BaseGlyphs.fromList(List(adv.judgment.glyph)), comp = true)
                      )
                    .flatMap(bsonWriteOpt)
                )
              )
          )

      saveAnalysisLine()
        .flatMap: updatedRoot =>
          saveInfoAdvice().inject(updatedRoot -> nextPath)

    end saveAnalysis

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

  def withAnalysis(chapter: Chapter, analysis: Analysis): Root =
    chapter.root.mainline
      .zip(analysis.infoAdvices)
      .foldLeft(chapter.root -> UciPath.root):
        case ((root, path), (node, (info, advOpt))) =>
          val nextPath = path + node.id
          val withLine = root
            .nodeAt(path)
            .flatMap(parent => analysisLine(parent, chapter.setup.variant, info))
            .map: line =>
              mergeAnalysis(root, path, line.copy(children = line.children.updateAllWith(_.setComp)).setComp)
            .getOrElse(root)
          val annotated = withLine.updateChildrenAt(nextPath, annotate(_, info, advOpt)) | withLine
          annotated -> nextPath
      ._1

  private[study] def mergeAnalysis(root: Root, path: UciPath, line: Branch): Root =
    root
      .nodeAt(path)
      .fold(root): parent =>
        val children = mergeAnalysisChildren(parent.children, line, parent.comp)
        if path.isEmpty then root.copy(children = children)
        else root.updateChildrenAt(path, _.copy(children = children)) | root

  private def mergeAnalysisChildren(
      children: lila.tree.Branches,
      line: Branch,
      isParentComp: Boolean
  ): lila.tree.Branches = children.get(line.id) match
    case None =>
      val nodes = children.toList
      val insertAt = isParentComp.option(nodes.indexWhere(!_.comp)).getOrElse(nodes.size)
      lila.tree.Branches:
        if insertAt < 0 then nodes :+ line
        else nodes.patch(insertAt, List(line), 0)
    case Some(existing) =>
      children.update(
        existing.copy(
          children = line.children.toList.foldLeft(existing.children): (merged, child) =>
            mergeAnalysisChildren(merged, child, isParentComp || line.comp)
        )
      )

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

  private def annotate(node: Branch, info: Info, advOpt: Option[Advice]): Branch =
    val withEval =
      if info.eval.score.isDefined && node.eval.isEmpty then
        node.copy(eval = info.eval.copy(static = true).some)
      else node
    advOpt.fold(withEval): adv =>
      val comments =
        if withEval.comments.hasComp then withEval.comments
        else
          withEval.comments
            + Comment(Comment.Id.make, adv.makeComment(false), Comment.Author.Lichess, comp = true)
      withEval.copy(
        comments = comments,
        glyphs = withEval.glyphs.merge(
          NodeGlyphs.fromBase(BaseGlyphs.fromList(List(adv.judgment.glyph)), comp = true)
        )
      )
