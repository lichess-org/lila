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

  def postGameStudy(
      game: Game,
      orientation: Color,
      ownerId: User.ID,
      users: List[User.ID] = Nil,
      withOpponent: Boolean = false
  ): Fu[Study.WithActualChapters] = {
    val pgs = Study.PostGameStudy(
      gameId = game.id,
      withOpponent = withOpponent,
      sentePlayer = Study.GamePlayer.fromGamePlayer(game.sentePlayer),
      gotePlayer = Study.GamePlayer.fromGamePlayer(game.gotePlayer)
    )
    val members = users.map(uid => StudyMember(uid, StudyMember.Role.Write))
    val study = Study.make(
      name = Study.Name(s"Post-game study - ${pgs.gameId}"),
      ownerId = ownerId,
      from = Study.From.Game(pgs.gameId),
      members = Option(members).filter(_.nonEmpty),
      postGameStudy = pgs.some
    )
    val pov = Pov(game, orientation)
    createPovChapters(pov, study.id, ownerId).map { chapters =>
      Study.WithActualChapters(chapters.headOption.fold(study)(study withChapter _), chapters)
    }
  }

  private def createFromScratch(data: StudyMaker.ImportGame, user: User): Fu[Study.WithActualChapters] = {
    val study = Study.make(
      name = data.name | Study.Name(s"${user.username}'s Study"),
      ownerId = user.id,
      from = Study.From.Scratch,
      id = data.id,
      settings = data.settings
    )
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
    val study = Study.make(
      name = Study.Name("Game study"),
      ownerId = user.id,
      from = Study.From.Game(pov.gameId),
      id = data.id
    )
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
      tags <- notationDump.tags(
        pov.game,
        shogi.format.Tag(_.TimeControl, pov.game.clock.fold("")(_.config.show))
      )
      name <- Namer.gameVsText(pov.game, withRatings = false)(lightUserApi.async)
      roots = makeRoots(pov)
      chapters = roots.zipWithIndex.map { case (r, i) =>
        val nameStr = if (roots.sizeIs > 1) s"$name pt.${i + 1}" else name
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
          tags = StudyTags(tags),
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
    if (pov.game.usis.sizeIs < Node.MAX_PLIES) Node.MAX_PLIES
    else Node.MAX_PLIES - 50

  private def makeClocks(pov: Pov): Option[Vector[Centis]] =
    for {
      initTime <- pov.game.clock.map(_.config.initTime)
      times    <- pov.game.bothClockStates
    } yield (initTime +: times)

  // studies are limited to 400 nodes, split longer games into multiple roots
  private def makeRoots(pov: Pov): List[Node.Root] = {
    val nodeLimit = chapterNodeLimit(pov)
    val usisList =
      if (pov.game.usis.nonEmpty)
        pov.game.usis.grouped(nodeLimit)
      else Iterator(Vector.empty[shogi.format.usi.Usi])
    val clocks = makeClocks(pov).map(_.grouped(nodeLimit).toVector)
    usisList
      .foldLeft(List.empty[Node.Root]) { case (acc, cur) =>
        val gm = Node.GameMainline(
          id = pov.game.id,
          part = acc.size,
          variant = pov.game.variant,
          usis = cur,
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
      from: Option[Study.From] = None
  )
}
