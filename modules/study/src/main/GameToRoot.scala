package lidraughts.study

import draughts.format.FEN
import lidraughts.game.Game
import lidraughts.round.JsonView.WithFlags

private object GameToRoot {

  def apply(game: Game, initialFen: Option[FEN], withClocks: Boolean): Node.Root = {
    val root = Node.Root.fromRoot {
      lidraughts.round.TreeBuilder(
        game = game,
        analysis = none,
        initialFen = initialFen | FEN(game.variant.initialFen),
        withFlags = WithFlags(clocks = withClocks)
      )
    }
    endComment(game).fold(root) { comment =>
      root updateMainlineLast { _.setComment(comment) }
    }
  }

  private def endComment(game: Game) = game.finished option {
    import lidraughts.tree.Node.Comment
    val result = draughts.Color.showResult(game.winnerColor)
    val status = lidraughts.game.StatusText(game)
    val text = s"$result $status"
    Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lidraughts)
  }
}
