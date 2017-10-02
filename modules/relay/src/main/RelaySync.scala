package lila.relay

import chess.format.pgn.Tag
import lila.common.{ LilaException, Chronometer }
import lila.socket.Socket.Uid
import lila.study._

private final class RelaySync(
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
) {

  def apply(relay: Relay, games: RelayGames): Funit = studyApi byId relay.studyId flatMap {
    _ ?? { study =>
      for {
        chapters <- chapterRepo orderedByStudy study.id
        _ <- lila.common.Future.traverseSequentially(games) { game =>
          chapters.find(_.tags(idTag) has game.id) match {
            case Some(chapter) => updateChapter(study, chapter, game)
            case None => createChapter(study, game) flatMap { chapter =>
              chapters.find(_.isEmptyInitial).ifTrue(chapter.order == 2) ?? { initial =>
                studyApi.deleteChapter(study.ownerId, study.id, initial.id, socketUid)
              }
            }
          }
        }
      } yield ()
    }
  }

  private def updateChapter(study: Study, chapter: Chapter, game: RelayGame): Funit = {
    game.root.mainline.foldLeft(Path.root -> none[Node]) {
      case ((parentPath, None), gameNode) =>
        val path = parentPath + gameNode
        chapter.root.nodeAt(path) match {
          case None => parentPath -> gameNode.some
          case Some(existing) =>
            gameNode.clock.filter(c => !existing.clock.has(c)) ?? { c =>
              studyApi.doSetClock(
                userId = chapter.ownerId,
                study = study,
                position = Position(chapter, path),
                clock = c.some,
                uid = socketUid
              )
            }
            path -> none
        }
      case (found, _) => found
    } match {
      // fix mainline, and call it a day
      case (path, _) if !Path.isMainline(chapter.root, path) => studyApi.promote(
        userId = chapter.ownerId,
        studyId = study.id,
        position = Position(chapter, path).ref,
        toMainline = true,
        uid = socketUid
      )
      case (path, None) => funit // no new nodes were found
      case (path, Some(node)) => // append new nodes to the chapter
        lila.common.Future.fold(node.mainline)(Position(chapter, path)) {
          case (position, n) => studyApi.doAddNode(
            userId = position.chapter.ownerId,
            study = study,
            position = position,
            node = n,
            uid = socketUid,
            opts = moveOpts.copy(clock = n.clock)
          ) flatten s"Can't add relay node $position $node"
        } void
    }
  }

  private def createChapter(study: Study, game: RelayGame): Fu[Chapter] =
    chapterRepo.nextOrderByStudy(study.id) flatMap { order =>
      val chapter = Chapter.make(
        studyId = study.id,
        name = Chapter.Name(s"${game.whiteName} - ${game.blackName}"),
        setup = Chapter.Setup(
          none,
          game.tags.variant | chess.variant.Variant.default,
          chess.Color.White
        ),
        root = game.root,
        tags = game.tags + Tag(idTag, game.id),
        order = order,
        ownerId = study.ownerId,
        practice = false,
        gamebook = false,
        conceal = none
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

  private val idTag = "RelayId"
}
