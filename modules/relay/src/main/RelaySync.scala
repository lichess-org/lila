package lila.relay

import org.joda.time.DateTime

import chess.format.pgn.{ Tag, Tags }
import lila.socket.Socket.Uid
import lila.study._

private final class RelaySync(
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
) {

  private type NbMoves = Int

  def apply(relay: Relay, games: RelayGames): Fu[SyncResult.Ok] =
    studyApi byId relay.studyId flatten "Missing relay study!" flatMap { study =>
      chapterRepo orderedByStudy study.id flatMap { chapters =>
        RelayInputSanity(chapters, games) match {
          case Some(fail) => fufail(fail.msg)
          case None => lila.common.Future.traverseSequentially(games) { game =>
            findCorrespondingChapter(game, chapters, games.size) match {
              case Some(chapter) => updateChapter(study, chapter, game)
              case None => createChapter(study, game) flatMap { chapter =>
                chapters.find(_.isEmptyInitial).ifTrue(chapter.order == 2).?? { initial =>
                  studyApi.deleteChapter(study.ownerId, study.id, initial.id, socketUid)
                } inject chapter.root.mainline.size
              }
            }
          } map { _.foldLeft(0)(_ + _) } map { SyncResult.Ok(_, games) }
        }
      }
    }

  /*
   * If the source contains several games, use their index to match them with the study chapter.
   * If the source contains only one game, use the player tags to match with the study chapter.
   * So the TCEC style - one game per file, reusing the file for all games - is supported.
   * lichess will create a new chapter when the game player tags differ.
   */
  private def findCorrespondingChapter(game: RelayGame, chapters: List[Chapter], nbGames: Int): Option[Chapter] =
    if (nbGames == 1) chapters find game.staticTagsMatch
    else chapters.find(_.relay.exists(_.index == game.index))

  private def updateChapter(study: Study, chapter: Chapter, game: RelayGame): Fu[NbMoves] =
    updateChapterTags(study, chapter, game) >>
      updateChapterTree(study, chapter, game)

  private def updateChapterTree(study: Study, chapter: Chapter, game: RelayGame): Fu[NbMoves] =
    game.root.mainline.foldLeft(Path.root -> none[Node]) {
      case ((parentPath, None), gameNode) =>
        val path = parentPath + gameNode
        chapter.root.nodeAt(path) match {
          case None => parentPath -> gameNode.some
          case Some(existing) =>
            gameNode.clock.filter(c => !existing.clock.has(c)) ?? { c =>
              studyApi.setClock(
                studyId = study.id,
                position = Position(chapter, path).ref,
                clock = c.some,
                uid = socketUid
              )
            }
            path -> none
        }
      case (found, _) => found
    } match {
      case (path, newNode) =>
        !Path.isMainline(chapter.root, path) ?? {
          logger.info(s"Change mainline ${showSC(study, chapter)} $path")
          studyApi.promote(
            userId = chapter.ownerId,
            studyId = study.id,
            position = Position(chapter, path).ref,
            toMainline = true,
            uid = socketUid
          ) >> chapterRepo.setRelayPath(chapter.id, path)
        } >> newNode.?? { node =>
          lila.common.Future.fold(node.mainline)(Position(chapter, path).ref) {
            case (position, n) => studyApi.addNode(
              userId = chapter.ownerId,
              studyId = study.id,
              position = position,
              node = n,
              uid = socketUid,
              opts = moveOpts.copy(clock = n.clock),
              relay = Chapter.Relay(
                index = game.index,
                path = position.path + n,
                lastMoveAt = DateTime.now
              ).some
            ) inject position + n
          } inject node.mainline.size
        }
    }

  private def updateChapterTags(study: Study, chapter: Chapter, game: RelayGame): Funit = {
    val gameTags = game.tags.value.foldLeft(Tags(Nil)) {
      case (newTags, tag) =>
        if (!chapter.tags.value.exists(tag ==)) newTags + tag
        else newTags
    }
    val tags = game.end
      .ifFalse(gameTags(_.Result).isDefined)
      .filterNot(end => chapter.tags(_.Result).??(end.resultText ==))
      .fold(gameTags) { end =>
        gameTags + Tag(_.Result, end.resultText)
      }
    val chapterNewTags = tags.value.foldLeft(chapter.tags) {
      case (chapterTags, tag) => PgnTags(chapterTags + tag)
    }
    (chapterNewTags != chapter.tags) ?? {
      if (vs(chapterNewTags) != vs(chapter.tags))
        logger.info(s"Update ${showSC(study, chapter)} tags '${vs(chapter.tags)}' -> '${vs(chapterNewTags)}'")
      studyApi.setTags(
        userId = chapter.ownerId,
        studyId = study.id,
        chapterId = chapter.id,
        tags = chapterNewTags,
        uid = socketUid
      ) >> {
        chapterNewTags.resultColor.isDefined ?? onChapterEnd(study.id, chapter.id)
      }
    }
  }

  private def onChapterEnd(studyId: Study.Id, chapterId: Chapter.Id): Funit =
    chapterRepo.setRelayPath(chapterId, Path.root) >>
      studyApi.analysisRequest(
        studyId = studyId,
        chapterId = chapterId,
        userId = "lichess"
      )

  private def createChapter(study: Study, game: RelayGame): Fu[Chapter] =
    chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
      val name = {
        for {
          w <- game.tags(_.White)
          b <- game.tags(_.Black)
        } yield s"$w - $b"
      } orElse game.tags("board") getOrElse "?"
      val chapter = Chapter.make(
        studyId = study.id,
        name = Chapter.Name(name),
        setup = Chapter.Setup(
          none,
          game.variant,
          chess.Color.White
        ),
        root = game.root,
        tags = game.tags,
        order = order,
        ownerId = study.ownerId,
        practice = false,
        gamebook = false,
        conceal = none,
        relay = Chapter.Relay(
          index = game.index,
          path = game.root.mainlinePath,
          lastMoveAt = DateTime.now
        ).some
      )
      studyApi.doAddChapter(study, chapter, sticky = false, uid = socketUid) inject chapter
    }

  private val moveOpts = MoveOpts(
    write = true,
    sticky = false,
    promoteToMainline = true,
    clock = none
  )

  private val socketUid = Uid("")

  private def vs(tags: Tags) = s"${tags(_.White) | "?"} - ${tags(_.Black) | "?"}"

  private def showSC(study: Study, chapter: Chapter) =
    s"#${study.id} chapter[${chapter.relay.fold("?")(_.index.toString)}]"
}

sealed trait SyncResult {
  val reportKey: String
}
object SyncResult {
  case class Ok(moves: Int, games: RelayGames) extends SyncResult {
    val reportKey = "ok"
  }
  case object Timeout extends Exception with SyncResult {
    val reportKey = "timeout"
    override def getMessage = "In progress..."
  }
  case class Error(msg: String) extends SyncResult {
    val reportKey = "error"
  }
}
