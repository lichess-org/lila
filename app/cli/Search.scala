package lila
package cli

import lila.search.SearchEnv
import lila.search.Query
import scalaz.effects._

private[cli] case class Search(env: SearchEnv) {

  def reset: IO[Unit] = env.indexer.rebuildAll

  private def showGame(game: lila.game.DbGame) =
    game.id + " " + game.turns //+ " " + game
}
