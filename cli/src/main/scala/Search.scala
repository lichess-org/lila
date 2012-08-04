package lila.cli

import lila.search.SearchEnv
import scalaz.effects._

case class Search(env: SearchEnv) {

  def reset: IO[Unit] = env.indexer.rebuildAll
}
