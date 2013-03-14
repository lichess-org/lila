package lila.wiki

import lila.db.ReactiveColl

import com.typesafe.config.Config

final class WikiEnv(config: Config, db: String ⇒ ReactiveColl) {

  private val settings = new Settings(config)
  import settings._

  lazy val api = new Api(pageRepo)

  private lazy val pageRepo = new PageRepo(db(CollectionPage))
  
  private lazy val fetcher = new Fetch(gitUrl = GitUrl, pageRepo = pageRepo)

  def cli = new {
    def fetch = WikiEnv.this.fetcher.apply inject "Fetched wiki from github"
  }
}
