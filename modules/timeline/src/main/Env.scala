package lidraughts.timeline

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    hub: lidraughts.hub.Env,
    getFriendIds: String => Fu[Set[String]],
    getFollowerIds: String => Fu[Set[String]],
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    renderer: ActorSelection,
    system: ActorSystem
) {

  private val CollectionEntry = config getString "collection.entry"
  private val CollectionUnsub = config getString "collection.unsub"
  private val UserDisplayMax = config getInt "user.display_max"
  private val UserActorName = config getString "user.actor.name"

  lazy val entryApi = new EntryApi(
    coll = entryColl,
    asyncCache = asyncCache,
    userMax = UserDisplayMax
  )

  system.actorOf(Props(new Push(
    bus = system.lidraughtsBus,
    renderer = renderer,
    getFriendIds = getFriendIds,
    getFollowerIds = getFollowerIds,
    unsubApi = unsubApi,
    entryApi = entryApi
  )), name = UserActorName)

  lazy val unsubApi = new UnsubApi(unsubColl)

  def isUnsub(channel: String)(userId: String): Fu[Boolean] =
    unsubApi.get(channel, userId)

  def status(channel: String)(userId: String): Fu[Option[Boolean]] =
    unsubApi.get(channel, userId) flatMap {
      case true => fuccess(Some(true)) // unsubed
      case false => entryApi.channelUserIdRecentExists(channel, userId) map {
        case true => Some(false) // subed
        case false => None // not applicable
      }
    }

  system.lidraughtsBus.subscribeFun('shadowban) {
    case lidraughts.hub.actorApi.mod.Shadowban(userId, true) => entryApi removeRecentFollowsBy userId
  }

  private[timeline] lazy val entryColl = db(CollectionEntry)
  private[timeline] lazy val unsubColl = db(CollectionUnsub)
}

object Env {

  lazy val current = "timeline" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "timeline",
    db = lidraughts.db.Env.current,
    hub = lidraughts.hub.Env.current,
    getFriendIds = lidraughts.relation.Env.current.api.fetchFriends,
    getFollowerIds = lidraughts.relation.Env.current.api.fetchFollowersFromSecondary,
    renderer = lidraughts.hub.Env.current.renderer,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system
  )
}
