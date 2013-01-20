package lila
package cli

import lila.forum.ForumEnv
import scalaz.effects._

private[cli] case class Forum(env: ForumEnv) {

  def denormalize: IO[String] = env.denormalize inject "Forum denormalized"

  def typecheck: IO[String] = 
    env.categRepo.all >> env.topicRepo.all >> env.postRepo.all inject "Forum type checked"

  def searchReset: IO[String] = env.indexer.rebuildAll inject "Search index reset"

  def search(text: String) = io {
    val paginator = env.searchPaginator(text, 1)
    (paginator.nbResults + " results") :: paginator.currentPageResults.map(_.show)
  } map (_ mkString "\n")
}
