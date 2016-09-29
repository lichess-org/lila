package lila.study

import chess.format.{ Forsyth, FEN }
import lila.game.{ GameRepo, Pov, Namer }
import lila.user.{ User, UserRepo }

private final class StudyMaker(
    lightUser: lila.common.LightUser.Getter,
    chapterMaker: ChapterMaker) {

  def apply(data: DataForm.Data, user: User): Fu[Study.WithChapter] =
    (data.gameId ?? GameRepo.gameWithInitialFen).flatMap {
      case Some((game, initialFen)) => createFromPov(Pov(game, data.orientation), initialFen, user)
      case None                     => createFromScratch(data, user)
    }

  private def createFromScratch(data: DataForm.Data, user: User): Fu[Study.WithChapter] = fuccess {
    val study = Study.make(user, Study.From.Scratch)
    val chapter = chapterMaker.fromFenOrPgnOrBlank(study, ChapterMaker.Data(
      game = none,
      name = "Chapter 1",
      variant = data.variantStr,
      fen = data.fenStr,
      pgn = data.pgnStr,
      orientation = data.orientation.name,
      conceal = false,
      initial = true),
      order = 1,
      userId = user.id)
    Study.WithChapter(study withChapter chapter, chapter)
  }

  private def createFromPov(pov: Pov, initialFen: Option[FEN], user: User): Fu[Study.WithChapter] =
    chapterMaker.game2root(pov.game, initialFen) map { root =>
      val study = Study.make(user, Study.From.Game(pov.game.id)).copy(name = "Game study")
      val chapter: Chapter = Chapter.make(
        studyId = study.id,
        name = Namer.gameVsText(pov.game, withRatings = false)(lightUser),
        setup = Chapter.Setup(
          gameId = pov.game.id.some,
          variant = pov.game.variant,
          orientation = pov.color),
        root = root,
        order = 1,
        ownerId = user.id,
        conceal = None)
      Study.WithChapter(study withChapter chapter, chapter)
    } addEffect { swc =>
      chapterMaker.notifyChat(swc.study, pov.game, user.id)
    }
}
