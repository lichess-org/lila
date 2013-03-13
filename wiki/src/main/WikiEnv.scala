package lila.wiki

import lila.db.ReactiveColl

final class WikiEnv(settings: Settings, db: String ⇒ ReactiveColl) {

  import settings._

  // lazy val pageRepo = new PageRepo(mongodb(WikiCollectionPage))

  // lazy val api = new Api(pageRepo = pageRepo)
  
  // lazy val fetch = new Fetch(
  //   gitUrl = WikiGitUrl,
  //   pageRepo = pageRepo)
}
