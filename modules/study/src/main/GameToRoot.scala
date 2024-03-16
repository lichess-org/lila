package lila.study

import chess.Outcome
import chess.format.Fen

import lila.game.Game
import lila.round.JsonView.WithFlags
import lila.tree.Node.Comment
import lila.tree.Root

private object GameToRoot:

  def apply(game: Game, initialFen: Option[Fen.Epd], withClocks: Boolean): Root =
    val root = lila.round.TreeBuilder(
      game = game,
      analysis = none,
      initialFen = initialFen | game.variant.initialFen,
      withFlags = WithFlags(clocks = withClocks)
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
