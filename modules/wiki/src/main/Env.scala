package lila.wiki

import com.typesafe.config.Config

import lila.db.api.$find
import tube.pageTube

final class Env(config: Config, db: lila.db.Env) {

  private val CollectionPage = config getString "collection.page"
  private val GitUrl = config getString "git.url"
  private val MarkdownPath = config getString "markdown_path"

  lazy val api = new Api

  private lazy val fetcher = new Fetch(gitUrl = GitUrl, markdownPath = MarkdownPath)(pageColl)

  private[wiki] lazy val pageColl = db(CollectionPage)

  def cli = new lila.common.Cli {
    def process = {
      case "wiki" :: "fetch" :: Nil =>
        fetcher.apply inject "Fetched wiki from github"
    }
  }
}

object Env {

  lazy val current = "[boot] wiki" describes new Env(
    config = lila.common.PlayApp loadConfig "wiki",
    db = lila.db.Env.current)
}
