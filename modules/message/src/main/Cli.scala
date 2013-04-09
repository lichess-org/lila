package lila.message

import play.api.libs.concurrent.Execution.Implicits._

private[message] final class Cli(env: Env) {

  // def denormalize: IO[String] = env.denormalize inject "Forum denormalized"

  // def typecheck: IO[String] = 
  //   env.categRepo.all >> env.topicRepo.all >> env.postRepo.all inject "Forum type checked"

  // def searchReset: IO[String] = env.indexer.rebuildAll inject "Search index reset"

  // def search(text: String) = io {
  //   val paginator = env.searchPaginator(text, 1, true)
  //   (paginator.nbResults + " results") :: paginator.currentPageResults.map(_.show)
  // } map (_ mkString "\n")
}
