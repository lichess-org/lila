package lila.study

import chess.Outcome
import chess.format.Fen

import lila.game.Game
import lila.tree.Node.Comment
import lila.tree.{ ExportOptions, TreeBuilder, Root }

private object GameToRoot:

  def apply(game: Game, initialFen: Option[Fen.Full], withClocks: Boolean): Root =
    val root = TreeBuilder(
      game = game,
      analysis = none,
      initialFen = initialFen | game.variant.initialFen,
      withFlags = ExportOptions(clocks = withClocks)
    )
    endComment(game).fold(root) { comment =>
      root.updateMainlineLast { _.setComment(comment) }
    }

  private def endComment(game: Game) =
    game.finished.option {
      val result = Outcome.showResult(Outcome(game.winnerColor).some)
      val status = lila.game.StatusText(game)
      val text   = s"$result $status"
      Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)
    }
