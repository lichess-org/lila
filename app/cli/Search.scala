package lila
package cli

import lila.search.SearchEnv
import lila.search.Query
import scalaz.effects._

private[cli] case class Search(env: SearchEnv) {

  def reset: IO[String] = env.indexer.rebuildAll inject "Search index reset"
}
