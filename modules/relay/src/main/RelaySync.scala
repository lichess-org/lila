package lila.relay

import chess.format.pgn.Tag
import lila.socket.Socket.Uid
import lila.study._

private final class RelaySync(
    studyApi: StudyApi,
    chapterRepo: ChapterRepo
) {

  def apply(relay: Relay, multiPgn: String): Funit = studyApi byId relay.studyId flatMap {
    _ ?? { study =>
      chapterRepo orderedByStudy study.id flatMap { chapters =>
        multiGamePgnToGames(multiPgn, logger.branch(relay.toString)).map { game =>
          chapters.find(_.tags(idTag) contains game.id) match {
            case Some(chapter) => updateChapter(study, chapter, game)
            case None => createChapter(study, game)
          }
        }.sequenceFu.void
      }
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
      case (_, None) => funit
      case (path, Some(node)) =>
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

  private def multiGamePgnToGames(multiPgn: String, logger: lila.log.Logger): List[RelayGame] =
    splitGames(multiPgn).flatMap { pgn =>
      PgnImport(pgn, Nil).fold(
        err => {
          logger.info(s"Invalid PGN $err")
          none
        },
        res => for {
          white <- res.tags(_.White)
          black <- res.tags(_.Black)
        } yield RelayGame(
          tags = res.tags,
          root = res.root,
          whiteName = RelayGame.PlayerName(white),
          blackName = RelayGame.PlayerName(black)
        )
      )
    }

  private def splitGames(multiPgn: String): List[String] =
    """\n\n\[""".r.split(multiPgn.replace("\r\n", "\n")).toList match {
      case first :: rest => first :: rest.map(t => s"[$t")
      case Nil => Nil
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
