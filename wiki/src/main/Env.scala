package lila.wiki

import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  private val settings = new Settings(config)
  import settings._

  lazy val api = new Api(pageRepo)

  private lazy val pageRepo = new PageRepo(db(CollectionPage))
  
  private lazy val fetcher = new Fetch(gitUrl = GitUrl, pageRepo = pageRepo)

  def cli = new {
    def fetch = fetcher.apply inject "Fetched wiki from github"
  }
}
