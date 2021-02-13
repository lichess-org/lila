package lila.study

import chess.format.FEN
import lila.game.{ Namer, Pov }
import lila.user.User

final private class StudyMaker(
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    chapterMaker: ChapterMaker,
    pgnDump: lila.game.PgnDump
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(data: StudyMaker.ImportGame, user: User): Fu[Study.WithChapter] =
    (data.form.gameId ?? gameRepo.gameWithInitialFen).flatMap {
      case Some((game, initialFen)) => createFromPov(data, Pov(game, data.form.orientation), initialFen, user)
      case None                     => createFromScratch(data, user)
    } map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  private def createFromScratch(data: StudyMaker.ImportGame, user: User): Fu[Study.WithChapter] = {
    val study = Study.make(user, Study.From.Scratch, data.id, data.name, data.settings)
    chapterMaker.fromFenOrPgnOrBlank(
      study,
      ChapterMaker.Data(
        game = none,
        name = Chapter.Name("Chapter 1"),
        variant = data.form.variantStr,
        fen = data.form.fen,
        pgn = data.form.pgnStr,
        orientation = data.form.orientation.name,
        mode = ChapterMaker.Mode.Normal.key,
        initial = true
      ),
      order = 1,
      userId = user.id
    ) map { chapter =>
      Study.WithChapter(study withChapter chapter, chapter)
    }
  }

  private def createFromPov(
      data: StudyMaker.ImportGame,
      pov: Pov,
      initialFen: Option[FEN],
      user: User
  ): Fu[Study.WithChapter] = {
    for {
      root <- chapterMaker.game2root(pov.game, initialFen)
      tags <- pgnDump.tags(pov.game, initialFen, none, withOpening = true)
      name <- Namer.gameVsText(pov.game, withRatings = false)(lightUserApi.async) dmap Chapter.Name.apply
      study = Study.make(user, Study.From.Game(pov.gameId), data.id, Study.Name("Game study").some)
      chapter = Chapter.make(
        studyId = study.id,
        name = name,
        setup = Chapter.Setup(
          gameId = pov.gameId.some,
          variant = pov.game.variant,
          orientation = pov.color
        ),
        root = root,
        tags = PgnTags(tags),
        order = 1,
        ownerId = user.id,
        practice = false,
        gamebook = false,
        conceal = None
      )
    } yield {
      Study.WithChapter(study withChapter chapter, chapter)
    }
  } addEffect { swc =>
    chapterMaker.notifyChat(swc.study, pov.game, user.id)
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
