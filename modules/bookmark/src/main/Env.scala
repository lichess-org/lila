package lila.bookmark

import akka.actor._
import com.typesafe.config.Config

import lila.hub.actorApi.bookmark._

final class Env(
    config: Config,
    system: ActorSystem,
    db: lila.db.Env
) {

  private val CollectionBookmark = config getString "collection.bookmark"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val ActorName = config getString "actor.name"

  private[bookmark] lazy val bookmarkColl = db(CollectionBookmark)

  lazy val paginator = new PaginatorBuilder(
    coll = bookmarkColl,
    maxPerPage = lila.common.MaxPerPage(PaginatorMaxPerPage)
  )

  lazy val api = new BookmarkApi(
    coll = bookmarkColl,
    paginator = paginator
  )

  system.actorOf(Props(new Actor {
    def receive = {
      case Toggle(gameId, userId) => api.toggle(gameId, userId)
      case Remove(gameId) => api removeByGameId gameId
    }
  }), name = ActorName)
}

object Env {

  lazy val current = "bookmark" boot new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current
  )
}
