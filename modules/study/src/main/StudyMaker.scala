package lila.study

import shogi.format.forsyth.Sfen
import lila.game.{ Namer, Pov }
import lila.user.User

final private class StudyMaker(
    lightUserApi: lila.user.LightUserApi,
    gameRepo: lila.game.GameRepo,
    chapterMaker: ChapterMaker,
    notationDump: lila.game.NotationDump
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(data: StudyMaker.ImportGame, user: User): Fu[Study.WithChapter] =
    (data.form.gameId ?? gameRepo.game).flatMap {
      case Some(game) => createFromPov(data, Pov(game, data.form.orientation), user)
      case None       => createFromScratch(data, user)
    } map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  private def createFromScratch(data: StudyMaker.ImportGame, user: User): Fu[Study.WithChapter] = {
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
      Study.WithChapter(study withChapter chapter, chapter)
    }
  }

  private def createFromPov(
      data: StudyMaker.ImportGame,
      pov: Pov,
      user: User
  ): Fu[Study.WithChapter] = {
    for {
      tags <- notationDump.tags(pov.game, withOpening = true, csa = false)
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
        root = GameToRoot(pov.game, withClocks = true),
        tags = KifTags(tags),
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
