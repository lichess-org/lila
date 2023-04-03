package lila.relay

import chess.format.pgn.{ Tag, Tags }
import chess.format.UciPath
import lila.socket.Socket.Sri
import lila.study.*

final private class RelaySync(
    studyApi: StudyApi,
    multiboard: StudyMultiBoard,
    chapterRepo: ChapterRepo,
    tourRepo: RelayTourRepo,
    leaderboard: RelayLeaderboardApi
)(using Executor):

  def apply(rt: RelayRound.WithTour, games: RelayGames): Fu[SyncResult.Ok] =
    studyApi byId rt.round.studyId orFail "Missing relay study!" flatMap { study =>
      chapterRepo orderedByStudy study.id flatMap { chapters =>
        RelayInputSanity(chapters, games) match
          case Left(fail) => fufail(fail.msg)
          case Right(games) =>
            lila.common.LilaFuture.linear(games) { game =>
              findCorrespondingChapter(game, chapters, games.size) match
                case Some(chapter) => updateChapter(rt.tour, study, chapter, game) dmap some
                case None =>
                  chapterRepo.countByStudyId(study.id) flatMap {
                    case nb if nb >= RelayFetch.maxChapters(rt.tour) => fuccess(none)
                    case _ =>
                      createChapter(study, game) flatMap { chapter =>
                        chapters.find(_.isEmptyInitial).ifTrue(chapter.order == 2).?? { initial =>
                          studyApi.deleteChapter(study.id, initial.id) {
                            actorApi.Who(study.ownerId, sri)
                          }
                        } inject SyncResult.ChapterResult(chapter.id, true, chapter.root.mainline.size).some
                      }
                  }
            } flatMap { chapterUpdates =>
              val result = SyncResult.Ok(chapterUpdates.toList.flatten, games)
              lila.common.Bus.publish(result, SyncResult busChannel rt.round.id)
              tourRepo.setSyncedNow(rt.tour) inject result
            }
      }
    }

  /*
   * If the source contains several games, use their index to match them with the study chapter.
   * If the source contains only one game, use the player tags (and site) to match with the study chapter.
   * So the TCEC style - one game per file, reusing the file for all games - is supported.
   * lichess will create a new chapter when the game player tags differ.
   */
  private def findCorrespondingChapter(
      game: RelayGame,
      chapters: List[Chapter],
      nbGames: Int
  ): Option[Chapter] =
    if (nbGames == 1 || game.looksLikeLichess) chapters find game.staticTagsMatch
    else chapters.find(_.relay.exists(_.index == game.index))

  private def updateChapter(
      tour: RelayTour,
      study: Study,
      chapter: Chapter,
      game: RelayGame
  ): Fu[SyncResult.ChapterResult] =
    updateChapterTags(tour, study, chapter, game) zip
      updateChapterTree(study, chapter, game) map { (tagUpdate, nbMoves) =>
        SyncResult.ChapterResult(chapter.id, tagUpdate, nbMoves)
      }

  private type NbMoves = Int
  private def updateChapterTree(study: Study, chapter: Chapter, game: RelayGame): Fu[NbMoves] =
    val who = actorApi.Who(chapter.ownerId, sri)
    game.root.mainline.foldLeft(UciPath.root -> none[Node]) {
      case ((parentPath, None), gameNode) =>
        val path = parentPath + gameNode.id
        chapter.root.nodeAt(path) match
          case None => parentPath -> gameNode.some
          case Some(existing) =>
            gameNode.clock.filter(c => !existing.clock.has(c)) ?? { c =>
              studyApi.setClock(
                studyId = study.id,
                position = Position(chapter, path).ref,
                clock = c.some
              )(who)
            }
            path -> none
      case (found, _) => found
    } match
      case (path, newNode) =>
        !path.isMainline(chapter.root) ?? {
          logger.info(s"Change mainline ${showSC(study, chapter)} $path")
          studyApi.promote(
            studyId = study.id,
            position = Position(chapter, path).ref,
            toMainline = true
          )(who) >> chapterRepo.setRelayPath(chapter.id, path)
        } >> newNode.?? { node =>
          lila.common.LilaFuture.fold(node.mainline.toList)(Position(chapter, path).ref) {
            case (position, n) =>
              studyApi.addNode(
                studyId = study.id,
                position = position,
                node = n,
                opts = moveOpts.copy(clock = n.clock),
                relay = Chapter
                  .Relay(
                    index = game.index,
                    path = position.path + n.id,
                    lastMoveAt = nowInstant
                  )
                  .some
              )(who) inject position + n
          } inject {
            if (chapter.root.children.nodes.isEmpty && node.mainline.nonEmpty)
              studyApi.reloadChapters(study)
            node.mainline.size
          }
        }

  private def updateChapterTags(
      tour: RelayTour,
      study: Study,
      chapter: Chapter,
      game: RelayGame
  ): Fu[Boolean] =
    val gameTags = game.tags.value.foldLeft(Tags(Nil)) { (newTags, tag) =>
      if (!chapter.tags.value.has(tag)) newTags + tag
      else newTags
    }
    val newEndTag = game.end
      .ifFalse(gameTags(_.Result).isDefined)
      .filterNot(end => chapter.tags(_.Result).??(end.resultText ==))
      .map(end => Tag(_.Result, end.resultText))
    val tags = newEndTag.fold(gameTags)(gameTags + _)
    val chapterNewTags = tags.value.foldLeft(chapter.tags) { (chapterTags, tag) =>
      PgnTags(chapterTags + tag)
    }
    (chapterNewTags != chapter.tags) ?? {
      if (vs(chapterNewTags) != vs(chapter.tags))
        logger.info(s"Update ${showSC(study, chapter)} tags '${vs(chapter.tags)}' -> '${vs(chapterNewTags)}'")
      studyApi.setTags(
        studyId = study.id,
        chapterId = chapter.id,
        tags = chapterNewTags
      )(actorApi.Who(chapter.ownerId, sri)) >> {
        val newEnd = chapter.tags.outcome.isEmpty && tags.outcome.isDefined
        newEnd ?? onChapterEnd(tour, study, chapter)
      } inject true
    }

  private def onChapterEnd(tour: RelayTour, study: Study, chapter: Chapter): Funit =
    chapterRepo.setRelayPath(chapter.id, UciPath.root) >> {
      (tour.official && chapter.root.mainline.sizeIs > 10) ?? studyApi.analysisRequest(
        studyId = study.id,
        chapterId = chapter.id,
        userId = study.ownerId,
        unlimited = true
      )
    } >>- {
      multiboard.invalidate(study.id)
      studyApi.reloadChapters(study)
      leaderboard invalidate tour.id
    }

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
        name = StudyChapterName(name),
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
        relay = Chapter
          .Relay(
            index = game.index,
            path = game.root.mainlinePath,
            lastMoveAt = nowInstant
          )
          .some
      )
      studyApi.doAddChapter(study, chapter, sticky = false, actorApi.Who(study.ownerId, sri)) >>-
        multiboard.invalidate(study.id) inject chapter
    }

  private val moveOpts = MoveOpts(
    write = true,
    sticky = false,
    promoteToMainline = true,
    clock = none
  )

  private val sri = Sri("")

  private def vs(tags: Tags) = s"${tags(_.White) | "?"} - ${tags(_.Black) | "?"}"

  private def showSC(study: Study, chapter: Chapter) =
    s"#${study.id} chapter[${chapter.relay.fold("?")(_.index.toString)}]"

sealed trait SyncResult:
  val reportKey: String
object SyncResult:
  case class Ok(chapters: List[ChapterResult], games: RelayGames) extends SyncResult:
    def nbMoves   = chapters.foldLeft(0)(_ + _.newMoves)
    val reportKey = "ok"
  case object Timeout extends Exception with SyncResult with util.control.NoStackTrace:
    val reportKey           = "timeout"
    override def getMessage = "In progress..."
  case class Error(msg: String) extends SyncResult:
    val reportKey = "error"

  case class ChapterResult(id: StudyChapterId, tagUpdate: Boolean, newMoves: Int)

  def busChannel(roundId: RelayRoundId) = s"relaySyncResult:$roundId"
