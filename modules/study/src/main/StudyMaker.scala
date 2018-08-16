package lidraughts.study

import draughts.format.FEN
import draughts.format.pdn.Tags
import lidraughts.game.{ GameRepo, Pov, Namer }
import lidraughts.user.User

private final class StudyMaker(
    lightUser: lidraughts.common.LightUser.GetterSync,
    chapterMaker: ChapterMaker
) {

  def apply(data: StudyMaker.Data, user: User): Fu[Study.WithChapter] =
    (data.form.gameId ?? GameRepo.gameWithInitialFen).flatMap {
      case Some((game, initialFen)) => createFromPov(data, Pov(game, data.form.orientation), initialFen, user)
      case None => createFromScratch(data, user)
    } map { sc =>
      // apply specified From if any
      sc.copy(study = sc.study.copy(from = data.from | sc.study.from))
    }

  private def createFromScratch(data: StudyMaker.Data, user: User): Fu[Study.WithChapter] = {
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
      userId = user.id)
    c map { chapter =>
      Study.WithChapter(study withChapter chapter, chapter)
    }
  }

  private def createFromPov(data: StudyMaker.Data, pov: Pov, initialFen: Option[FEN], user: User): Fu[Study.WithChapter] =
    chapterMaker.game2root(pov.game, initialFen) map { root =>
      val study = Study.make(user, Study.From.Game(pov.game.id), data.id, Study.Name("Game study").some)
      val chapter: Chapter = Chapter.make(
        studyId = study.id,
        name = Chapter.Name(Namer.gameVsText(pov.game, withRatings = false)(lightUser)),
        setup = Chapter.Setup(
          gameId = pov.game.id.some,
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
