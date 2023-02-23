package lila.study

import lila.game.{ Game, Namer, Pov }
import lila.user.User

import shogi.Centis
import shogi.Color

final private class StudyMaker(
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    chapterMaker: ChapterMaker,
    notationDump: lila.game.NotationDump
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(data: StudyMaker.ImportGame, user: User): Fu[Study.WithActualChapters] =
    (data.form.gameId ?? gameRepo.game).flatMap {
      case Some(game) => createFromPov(data, Pov(game, data.form.orientation), user)
      case None       => createFromScratch(data, user)
    } map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  def postGameStudy(gameId: Game.ID, users: List[User.ID]): Fu[Option[Study.WithActualChapters]] = {
    gameRepo.finished(gameId).flatMap {
      _ ?? { g =>
        val study = Study.makePostGameStudy(g, users)
        val pov   = Pov(g, Color.Sente)
        createPovChapters(pov, study.id, User.lishogiId).map { chapters =>
          Study.WithActualChapters(chapters.headOption.fold(study)(study withChapter _), chapters).some
        }
      }
    }
  }

  private def createFromScratch(data: StudyMaker.ImportGame, user: User): Fu[Study.WithActualChapters] = {
    val study = Study.make(user, Study.From.Scratch, data.id, data.name, data.settings)
    chapterMaker.fromSfenOrNotationOrBlank(
      study,
      ChapterMaker.Data(
        game = none,
        name = Chapter.Name("Chapter 1"),
        variant = data.form.variantStr,
        sfen = data.form.sfen,
        notation = data.form.notationStr,
        orientation = data.form.orientation.name,
        mode = ChapterMaker.Mode.Normal.key,
        initial = true
      ),
      order = 1,
      userId = user.id
    ) map { chapter =>
      Study.WithActualChapters(study withChapter chapter, List(chapter))
    }
  }

  private def createFromPov(
      data: StudyMaker.ImportGame,
      pov: Pov,
      user: User
  ): Fu[Study.WithActualChapters] = {
    val study = Study.make(user, Study.From.Game(pov.gameId), data.id, Study.Name("Game study").some)
    createPovChapters(pov, study.id, user.id).map { chapters =>
      Study.WithActualChapters(chapters.headOption.fold(study)(study withChapter _), chapters)
    }
  }

  private def createPovChapters(
      pov: Pov,
      studyId: Study.Id,
      ownerId: User.ID
  ): Fu[List[Chapter]] =
    for {
      tags <- notationDump.tags(pov.game, withOpening = true, csa = false)
      name <- Namer.gameVsText(pov.game, withRatings = false)(lightUserApi.async)
      roots = makeRoots(pov)
      chapters = roots.zipWithIndex.map { case (r, i) =>
        val nameStr = if (roots.size > 1) s"$name pt.${i + 1}" else name
        Chapter.make(
          studyId = studyId,
          name = Chapter.Name(nameStr),
          setup = Chapter.Setup(
            gameId = pov.gameId.some,
            variant = pov.game.variant,
            orientation = pov.color,
            endStatus =
              ((roots.size - 1) == i) option Chapter.EndStatus(pov.game.status, pov.game.winnerColor)
          ),
          root = r,
          tags = KifTags(tags),
          order = 1,
          ownerId = ownerId,
          practice = false,
          gamebook = false,
          conceal = None
        )
      }
    } yield chapters

  // Make the potential next chapters reasonably long in case the game is close to Node.MAX_PLIES
  // and allow making moves at the chapter end
  private def chapterNodeLimit(pov: Pov): Int =
    if (pov.game.usiMoves.size < Node.MAX_PLIES) Node.MAX_PLIES
    else Node.MAX_PLIES - 50

  private def makeClocks(pov: Pov): Option[Vector[Centis]] =
    for {
      initTime <- pov.game.clock.map(c => Centis.ofSeconds(c.limitSeconds))
      times    <- pov.game.bothClockStates
    } yield (initTime +: times)

  // studies are limited to 400 nodes, split longer games into multiple roots
  private def makeRoots(pov: Pov): List[Node.Root] = {
    val nodeLimit    = chapterNodeLimit(pov)
    val usiMovesList = pov.game.usiMoves.grouped(nodeLimit)
    val clocks       = makeClocks(pov).map(_.grouped(nodeLimit).toVector)
    usiMovesList
      .foldLeft(List.empty[Node.Root]) { case (acc, cur) =>
        val gm = Node.GameMainline(
          id = pov.game.id,
          part = acc.size,
          variant = pov.game.variant,
          usiMoves = cur,
          initialSfen = acc.headOption.map(_.lastMainlineNode.sfen).orElse(pov.game.initialSfen),
          clocks = clocks.flatMap(_.lift(acc.size))
        )
        GameRootCache(gm) :: acc
      }
      .reverse
  }
}

object StudyMaker {

  case class ImportGame(
      form: StudyForm.importGame.Data = StudyForm.importGame.Data(),
      id: Option[Study.Id] = None,
      name: Option[Study.Name] = None,
      settings: Option[Settings] = None,
      from: Option[Study.From] = None,
      postGameStudy: Option[Study.PostGameStudy] = None
  )
}
