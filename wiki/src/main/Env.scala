package lila.wiki

import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  private val CollectionPage = config getString "collection.page"
  private val GitUrl = config getString "git.url"

  lazy val api = new Api()(pageColl)

  private[wiki] lazy val pageColl = db(CollectionPage)
  
  private lazy val fetcher = new Fetch(gitUrl = GitUrl)(pageColl)

  def cli = new {
    def fetch = fetcher.apply inject "Fetched wiki from github"
  }
}

object Env {

  lazy val current = "[wiki] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "wiki",
    db = lila.db.Env.current)
}
