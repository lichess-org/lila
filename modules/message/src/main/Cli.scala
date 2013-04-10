package lila.message

private[message] final class Cli(env: Env) {

  // TODO
  // def denormalize: IO[String] = env.denormalize inject "Forum denormalized"

  // def typecheck: IO[String] = 
  //   env.categRepo.all >> env.topicRepo.all >> env.postRepo.all inject "Forum type checked"

  // def searchReset: IO[String] = env.indexer.rebuildAll inject "Search index reset"

  // def search(text: String) = io {
  //   val paginator = env.searchPaginator(text, 1, true)
  //   (paginator.nbResults + " results") :: paginator.currentPageResults.map(_.show)
  // } map (_ mkString "\n")
}
