package lila.wiki

import tube.pageTube
import lila.db.api.$find

import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  private val CollectionPage = config getString "collection.page"
  private val GitUrl = config getString "git.url"

  def show(slug: String): Fu[Option[(Page, List[Page])]] = {
    import makeTimeout.short
    ($find.all).await.pp
    throw new Exception("aaarg")
    $find byId slug zip $find.all map {
      case (page, pages) ⇒ page map { _ -> pages }
    }
  }

  private[wiki] lazy val pageColl = db(CollectionPage)

  private lazy val fetcher = new Fetch(gitUrl = GitUrl)(pageColl)

  def cli = new lila.common.Cli {
    def process = {
      case "wiki" :: "fetch" :: Nil ⇒
        fetcher.apply inject "Fetched wiki from github"
    }
  }
}

object Env {

  lazy val current = "[boot] wiki" describes new Env(
    config = lila.common.PlayApp loadConfig "wiki",
    db = lila.db.Env.current)
}
