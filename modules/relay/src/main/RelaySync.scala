package lila.relay

import chess.format.pgn.Tag
import lila.common.LilaException
import lila.socket.Socket.Uid
import lila.study._
import scala.util.{ Try, Success, Failure }

private final class RelaySync(
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
) {

  import RelaySync._

  def apply(relay: Relay, multiPgn: MultiPgn): Funit = studyApi byId relay.studyId flatMap {
    _ ?? { study =>
      for {
        chapters <- chapterRepo orderedByStudy study.id
        games <- multiGamePgnToGames(multiPgn, logger.branch(relay.toString)).future
        _ <- lila.common.Future.traverseSequentially(games) { game =>
          chapters.find(_.tags(idTag) contains game.id) match {
            case Some(chapter) => updateChapter(study, chapter, game)
            case None => createChapter(study, game)
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

  private def createChapter(study: Study, game: RelayGame): Funit =
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
      studyApi.doAddChapter(study, chapter, sticky = false, uid = socketUid)
    }

  private def multiGamePgnToGames(multiPgn: MultiPgn, logger: lila.log.Logger): Try[List[RelayGame]] =
    multiPgn.value.foldLeft[Try[List[RelayGame]]](Success(List.empty)) {
      case (Success(acc), pgn) => for {
        res <- PgnImport(pgn, Nil).fold(
          err => Failure(LilaException(err)),
          Success.apply
        )
        white <- res.tags(_.White) toTry LilaException("Missing PGN White tag")
        black <- res.tags(_.Black) toTry LilaException("Missing PGN Black tag")
      } yield RelayGame(
        tags = res.tags,
        root = res.root,
        whiteName = RelayGame.PlayerName(white),
        blackName = RelayGame.PlayerName(black)
      ) :: acc
      case (acc, _) => acc
    }.map(_.reverse)

  private val moveOpts = MoveOpts(
    write = true,
    sticky = false,
    promoteToMainline = true,
    clock = none
  )

  private val socketUid = Uid("")

  private val idTag = "RelayId"
}

private object RelaySync {

  case class MultiPgn(value: List[String]) extends AnyVal
}
