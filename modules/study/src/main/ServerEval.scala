package lila.study

import chess.format.pgn.Glyphs as BaseGlyphs
import chess.format.{ Fen, Uci, UciPath }
import play.api.libs.json.*

import lila.core.perm.Granter
import lila.core.relay.GetCrowd
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
            _ <- chapterRepo.updateServerEval(chapter.id, false, chapter.root.mainlinePath.some)
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
            val merged = replace(chapter, analysis.some).copy(
              analysisGameId = none,
              serverEval = Chapter
                .ServerEval(
                  path = chapter.serverEval.fold(chapter.root.mainlinePath)(_.path),
                  done = complete
                )
                .some
            )
            for
              _ <- chapterRepo.update(merged)
              _ <- sendProgress(studyId, chapterId, analysis).logFailure(logger)
            yield ()
      case _ => funit

    def remove(studyId: StudyId, chapterId: StudyChapterId): Funit =
      sequencer.sequenceStudyWithChapter(studyId, chapterId):
        case Study.WithChapter(_, chapter) =>
          for _ <- chapterRepo.update(replace(chapter, none))
          yield lila.common.Bus.pub(lila.core.fishnet.Bus.StudyChapterDelete(chapter.id :: Nil))

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

  case class Progress(
      chapterId: StudyChapterId,
      tree: Root,
      analysis: JsObject,
      division: chess.Division
  )

  def replace(chapter: Chapter, analysis: Option[Analysis]): Chapter =
    val cleanRoot = clearAnalysis(chapter.root, analysis.fold(Set.empty)(incomingCompPaths(chapter, _)))
    analysis.fold(chapter.copy(root = cleanRoot, analysisGameId = none, serverEval = none)): analysis =>
      chapter.copy(root = withAnalysis(cleanRoot, chapter.setup.variant, analysis), analysisGameId = none)

  private def withAnalysis(
      cleanRoot: Root,
      variant: chess.variant.Variant,
      analysis: Analysis
  ): Root = cleanRoot.mainline
    .zip(analysis.infoAdvices)
    .foldLeft(cleanRoot -> UciPath.root):
      case ((root, path), (node, (info, advOpt))) =>
        val nextPath = path + node.id
        val withLine = root
          .nodeAt(path)
          .flatMap(analysisLine(_, variant, info))
          .map: line =>
            mergeAnalysis(
              root,
              path,
              line.copy(children = line.children.updateAllWith(_.setComp)).setComp
            )
          .getOrElse(root)
        val annotated = withLine.updateChildrenAt(nextPath, annotate(_, info, advOpt)) | withLine
        annotated -> nextPath
    ._1

  private def incomingCompPaths(chapter: Chapter, analysis: Analysis): Set[UciPath] =
    @annotation.tailrec
    def loop(
        nodes: List[Branch],
        infoAdvices: lila.tree.InfoAdvices,
        path: UciPath,
        paths: Set[UciPath]
    ): Set[UciPath] =
      (nodes, infoAdvices) match
        case (node :: restNodes, (info, _) :: restInfoAdvices) =>
          val linePaths = chapter.root
            .nodeAt(path)
            .flatMap(analysisLine(_, chapter.setup.variant, info))
            .map(compPaths(path, _))
            .getOrElse(Set.empty)
          loop(restNodes, restInfoAdvices, path + node.id, paths ++ linePaths)
        case _ => paths

    loop(chapter.root.mainline, analysis.infoAdvices, UciPath.root, Set.empty)

  private def compPaths(parentPath: UciPath, line: Branch): Set[UciPath] =
    val path = parentPath + line.id
    line.children.toList.foldLeft(Set(path))(_ ++ compPaths(path, _))

  private[study] def clearAnalysis(root: Root, incomingCompPaths: Set[UciPath] = Set.empty): Root =
    def cleanChildren(children: lila.tree.Branches, parentPath: UciPath): lila.tree.Branches =
      lila.tree.Branches:
        children.toList.flatMap: node =>
          val path = parentPath + node.id
          val cleaned = node.copy(
            eval = node.eval.filterNot(_.static),
            comments = node.comments.withoutComp,
            glyphs = node.glyphs.withoutComp,
            children = cleanChildren(node.children, path)
          )
          val hasUserContent =
            cleaned.children.hasNonComp || cleaned.eval.isDefined || cleaned.shapes.value.nonEmpty ||
              cleaned.comments.value.nonEmpty || cleaned.gamebook.isDefined || cleaned.glyphs.value.nonEmpty ||
              cleaned.clock.isDefined || cleaned.forceVariation
          if node.comp && !hasUserContent
          then none
          else cleaned.copy(comp = node.comp && incomingCompPaths.contains(path)).some

    root.copy(
      eval = root.eval.filterNot(_.static),
      comments = root.comments.withoutComp,
      glyphs = root.glyphs.withoutComp,
      children = cleanChildren(root.children, UciPath.root)
    )

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
