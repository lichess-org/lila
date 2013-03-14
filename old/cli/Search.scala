package lila.app
package cli

import lila.app.search.SearchEnv
import lila.app.search.Query
import scalaz.effects._

private[cli] case class Search(env: SearchEnv) {

  def reset: IO[String] = env.indexer.rebuildAll inject "Search index reset"
}
