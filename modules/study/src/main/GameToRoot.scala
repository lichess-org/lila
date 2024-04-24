package lila.study

import chess.Outcome
import chess.format.Fen

import lila.tree.Node.Comment
import lila.tree.{ ExportOptions, TreeBuilder, Root }

object GameToRoot:

  def apply(game: Game, initialFen: Option[Fen.Full], withClocks: Boolean): Root =
    val root = TreeBuilder(
      game = game,
      analysis = none,
      initialFen = initialFen | game.variant.initialFen,
      withFlags = ExportOptions(clocks = withClocks),
      logChessError = logChessError
    )
    endComment(game).fold(root)(comment => root.updateMainlineLast(_.setComment(comment)))

  private def endComment(game: Game) =
    game.finished.option:
      val result = Outcome.showResult(Outcome(game.winnerColor).some)
      val status = lila.tree.StatusText(game.status, game.winnerColor, game.variant)
      val text   = s"$result $status"
      Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)

  private val logChessError = (id: GameId) =>
    val logger = lila.log("study")
    (err: chess.ErrorStr) =>
      logger.warn(s"study.TreeBuilder https://lichess.org/$id ${err.value.linesIterator.toList.headOption}")
