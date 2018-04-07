package lidraughts.study

import draughts.format.FEN
import draughts.format.pdn.Tags
import lidraughts.game.{ GameRepo, Pov, Namer }
import lidraughts.user.User

private final class StudyMaker(
    lightUser: lidraughts.common.LightUser.GetterSync,
    chapterMaker: ChapterMaker
) {

  def apply(data: StudyMaker.Data, user: User, draughtsResult: Boolean): Fu[Study.WithChapter] =
    (data.form.gameId ?? GameRepo.gameWithInitialFen).flatMap {
      case Some((game, initialFen)) => createFromPov(data, Pov(game, data.form.orientation), initialFen, user, draughtsResult)
      case None => createFromScratch(data, user, draughtsResult)
    } map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  private def createFromScratch(data: StudyMaker.Data, user: User, draughtsResult: Boolean): Fu[Study.WithChapter] = {
    val study = Study.make(user, Study.From.Scratch, data.id, data.name, data.settings)
    val c = chapterMaker.fromFenOrPdnOrBlank(study, ChapterMaker.Data(
      game = none,
      name = Chapter.Name("Chapter 1"),
      variant = data.form.variantStr,
      fen = data.form.fenStr,
      pdn = data.form.pdnStr,
      orientation = data.form.orientation.name,
      mode = ChapterMaker.Mode.Normal.key,
      initial = true
    ),
      order = 1,
      userId = user.id,
      draughtsResult = draughtsResult)
    c map { chapter =>
      Study.WithChapter(study withChapter chapter, chapter)
    }
  }

  private def createFromPov(data: StudyMaker.Data, pov: Pov, initialFen: Option[FEN], user: User, draughtsResult: Boolean): Fu[Study.WithChapter] =
    chapterMaker.game2root(pov.game, initialFen, draughtsResult) map { root =>
      val study = Study.make(user, Study.From.Game(pov.gameId), data.id, Study.Name("Game study").some)
      val chapter: Chapter = Chapter.make(
        studyId = study.id,
        name = Chapter.Name(Namer.gameVsText(pov.game, withRatings = false)(lightUser)),
        setup = Chapter.Setup(
          gameId = pov.gameId.some,
          variant = pov.game.variant,
          orientation = pov.color
        ),
        root = root,
        tags = Tags.empty,
        order = 1,
        ownerId = user.id,
        practice = false,
        gamebook = false,
        conceal = None
      )
      Study.WithChapter(study withChapter chapter, chapter)
    } addEffect { swc =>
      chapterMaker.notifyChat(swc.study, pov.game, user.id)
    }
}

object StudyMaker {

  case class Data(
      form: DataForm.Data = DataForm.Data(),
      id: Option[Study.Id] = None,
      name: Option[Study.Name] = None,
      settings: Option[Settings] = None,
      from: Option[Study.From] = None
  )
}
