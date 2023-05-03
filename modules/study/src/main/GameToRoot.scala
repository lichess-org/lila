package lila.study

import chess.format.Fen
import lila.game.Game
import lila.round.JsonView.WithFlags
import lila.tree.Node.Comment
import chess.Outcome
import lila.tree.NewRoot

private object GameToRoot:

  def apply(game: Game, initialFen: Option[Fen.Epd], withClocks: Boolean): NewRoot =
    val root: NewRoot = lila.round.TreeBuilder(
      game = game,
      analysis = none,
      initialFen = initialFen | game.variant.initialFen,
      withFlags = WithFlags(clocks = withClocks)
    )
    endComment(game).fold(root)(comment => root.modifyLastMainlineOrRoot(_.setComment(comment)))

  private def endComment(game: Game) =
    game.finished option {
      val result = Outcome.showResult(Outcome(game.winnerColor).some)
      val status = lila.game.StatusText(game)
      val text   = s"$result $status"
      Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)
    }
