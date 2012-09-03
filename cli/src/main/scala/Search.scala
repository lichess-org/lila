package lila.cli

import lila.search.SearchEnv
import scalaz.effects._

case class Search(env: SearchEnv) {

  def reset: IO[Unit] = env.indexer.rebuildAll

  def test: IO[Unit] = env.indexer.searchTest flatMap { games ⇒
    putStrLn(games map (_.turns) mkString "\n")
  }
}
