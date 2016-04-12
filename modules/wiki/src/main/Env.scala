package lila.wiki

import com.typesafe.config.Config

import lila.db.dsl._

final class Env(config: Config, db: lila.db.Env) {

  private val CollectionPage = config getString "collection.page"
  private val GitUrl = config getString "git.url"
  private val MarkdownPath = config getString "markdown_path"

  private lazy val pageColl = db(CollectionPage)

  lazy val api = new Api(pageColl)

  private lazy val fetcher = new Fetch(
    coll = pageColl,
    gitUrl = GitUrl,
    markdownPath = MarkdownPath)

  def cli = new lila.common.Cli {
    def process = {
      case "wiki" :: "fetch" :: Nil =>
        fetcher.apply inject "Fetched wiki from github"
    }
  }
}

object Env {

  lazy val current = "wiki" boot new Env(
    config = lila.common.PlayApp loadConfig "wiki",
    db = lila.db.Env.current)
}
