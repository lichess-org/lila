package lila.study

import chess.Outcome
import chess.format.Fen
import chess.format.pgn.Comment as CommentStr

import lila.tree.Node.Comment
import lila.tree.{ ExportOptions, Root, TreeBuilder }

object GameToRoot:

  def apply(
      game: Game,
      initialFen: Option[Fen.Full],
      withClocks: Boolean,
      withEndComment: Boolean = true
  ): Root =
    val root = TreeBuilder(
      game = game,
      analysis = none,
      initialFen = initialFen | game.variant.initialFen,
      withFlags = ExportOptions(clocks = withClocks),
      logChessError = logger.warn
    )
    if withEndComment then
      endComment(game).fold(root)(comment => root.updateMainlineLast(_.setComment(comment)))
    else root

  private def endComment(game: Game) =
    game.finished.option:
      val result = Outcome.showResult(Outcome(game.winnerColor).some)
      val status = lila.tree.StatusText(game.status, game.winnerColor, game.variant)
      val text = s"$result $status"
      Comment(Comment.Id.make, CommentStr(text), Comment.Author.Lichess)
