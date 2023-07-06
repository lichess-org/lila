package lila.study

import chess.format.Fen
import lila.game.{ Game, Namer, Pov }
import lila.user.User
import lila.tree.{ Branch, Branches, Root }

final private class StudyMaker(
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    chapterMaker: ChapterMaker,
    pgnDump: lila.game.PgnDump
)(using Executor):

  def apply(data: StudyMaker.ImportGame, user: User, withRatings: Boolean): Fu[Study.WithChapter] =
    (data.form.gameId so gameRepo.gameWithInitialFen).flatMap {
      case Some(Game.WithInitialFen(game, initialFen)) =>
        createFromPov(
          data,
          Pov(game, data.form.orientation.flatMap(_.resolve) | chess.White),
          initialFen,
          user,
          withRatings
        )
      case None => createFromScratch(data, user)
    } map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  private def createFromScratch(data: StudyMaker.ImportGame, user: User): Fu[Study.WithChapter] =
    val study = Study.make(user, Study.From.Scratch, data.id, data.name, data.settings)
    chapterMaker.fromFenOrPgnOrBlank(
      study,
      ChapterMaker.Data(
        game = none,
        name = StudyChapterName("Chapter 1"),
        variant = data.form.variant,
        fen = data.form.fen,
        pgn = data.form.pgnStr,
        orientation = data.form.orientation | ChapterMaker.Orientation.Auto,
        mode = ChapterMaker.Mode.Normal,
        initial = true
      ),
      order = 1,
      userId = user.id
    ) map { chapter =>
      Study.WithChapter(study withChapter chapter, chapter)
    }

  private def createFromPov(
      data: StudyMaker.ImportGame,
      pov: Pov,
      initialFen: Option[Fen.Epd],
      user: User,
      withRatings: Boolean
  ): Fu[Study.WithChapter] = {
    for
      root <- chapterMaker.makeRoot(pov.game, data.form.pgnStr, initialFen)
      tags <- pgnDump.tags(pov.game, initialFen, none, withOpening = true, withRatings)
      name <- StudyChapterName from Namer.gameVsText(pov.game, withRatings)(using lightUserApi.async)
      study = Study.make(user, Study.From.Game(pov.gameId), data.id, StudyName("Game study").some)
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
    yield Study.WithChapter(study withChapter chapter, chapter)
  } addEffect { swc =>
    chapterMaker.notifyChat(swc.study, pov.game, user.id)
  }

object StudyMaker:

  case class ImportGame(
      form: StudyForm.importGame.Data = StudyForm.importGame.Data(),
      id: Option[StudyId] = None,
      name: Option[StudyName] = None,
      settings: Option[Settings] = None,
      from: Option[Study.From] = None
  )
