package lila.study

import chess.format.FEN
import chess.format.pgn.Tags
import lila.game.{ GameRepo, Pov, Namer }
import lila.user.User

private final class StudyMaker(
    lightUser: lila.common.LightUser.GetterSync,
    chapterMaker: ChapterMaker
) {

  def apply(data: DataForm.Data, user: User, forceId: Option[Study.Id] = None): Fu[Study.WithChapter] =
    (data.gameId ?? GameRepo.gameWithInitialFen).flatMap {
      case Some((game, initialFen)) => createFromPov(Pov(game, data.orientation), initialFen, user, forceId)
      case None => createFromScratch(data, user, forceId)
    }

  private def createFromScratch(data: DataForm.Data, user: User, forceId: Option[Study.Id] = None): Fu[Study.WithChapter] = {
    val study = Study.make(user, Study.From.Scratch, forceId)
    chapterMaker.fromFenOrPgnOrBlank(study, ChapterMaker.Data(
      game = none,
      name = Chapter.Name("Chapter 1"),
      variant = data.variantStr,
      fen = data.fenStr,
      pgn = data.pgnStr,
      orientation = data.orientation.name,
      mode = ChapterMaker.Mode.Normal.key,
      initial = true
    ),
      order = 1,
      userId = user.id) map { chapter =>
      Study.WithChapter(study withChapter chapter, chapter)
    }
  }

  private def createFromPov(pov: Pov, initialFen: Option[FEN], user: User, forceId: Option[Study.Id] = None): Fu[Study.WithChapter] =
    chapterMaker.game2root(pov.game, initialFen) map { root =>
      val study = Study.make(user, Study.From.Game(pov.game.id), forceId).copy(name = Study.Name("Game study"))
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
