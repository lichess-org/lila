package lila
package core

import com.typesafe.config.Config

final class Settings(config: Config) {

  import config._

  val SiteUidTimeout = millis("site.uid.timeout")

  val MonitorTimeout = millis("monitor.timeout")

  val I18nWebPathFsRelative = getString("i18n.web_path.fs.relative")

  val GameMessageLifetime = millis("game.message.lifetime")
  val GameUidTimeout = millis("game.uid.timeout")
  val GameHubTimeout = millis("game.hub.timeout")
  val GamePlayerTimeout = millis("game.player.timeout")
  val GameAnimationDelay = millis("game.animation.delay")
  val GameCachedNbTtl = millis("game.cached.nb.ttl")
  val GamePaginatorMaxPerPage = getInt("game.paginator.max_per_page")

  val UserPaginatorMaxPerPage = getInt("user.paginator.max_per_page")
  val UserEloUpdaterFloor = getInt("user.elo_updater.floor")
  val UserCachedNbTtl = millis("user.cached.nb.ttl")

  val ForumTopicMaxPerPage = getInt("forum.topic.max_per_page")
  val ForumPostMaxPerPage = getInt("forum.post.max_per_page")
  val ForumRecentTimeout = millis("forum.recent.timeout")

  val MessageThreadMaxPerPage = getInt("message.thread.max_per_page")

  val SetupFriendConfigMemoTtl = millis("setup.friend_config.memo.ttl")

  val LobbyEntryMax = getInt("lobby.entry.max")
  val LobbyMessageMax = getInt("lobby.message.max")
  val LobbyMessageLifetime = millis("lobby.message.lifetime")

  val MemoHookTimeout = millis("memo.hook.timeout")
  val MemoUsernameTimeout = millis("memo.username.timeout")

  val MoretimeSeconds = seconds("moretime.seconds")

  val FinisherLockTimeout = millis("memo.finisher_lock.timeout")

  val AiChoice = getString("ai.use")
  val AiServerMode = getBoolean("ai.server")
  val AiRemoteUrl = getString("ai.remote.url")
  val AiCraftyExecPath = getString("ai.crafty.exec_path")
  val AiCraftyBookPath = Some(getString("ai.crafty.book_path")) filter ("" !=)
  val AiRemote = "remote"
  val AiCrafty = "crafty"

  val MongoHost = getString("mongo.host")
  val MongoPort = getInt("mongo.port")
  val MongoDbName = getString("mongo.dbName")
  val MongoConnectionsPerHost = getInt("mongo.connectionsPerHost")
  val MongoAutoConnectRetry = getBoolean("mongo.autoConnectRetry")
  val MongoConnectTimeout = millis("mongo.connectTimeout")
  val MongoBlockingThreads = getInt("mongo.threadsAllowedToBlockForConnectionMultiplier")

  val MongoCollectionGame = getString("mongo.collection.game")
  val MongoCollectionHook = getString("mongo.collection.hook")
  val MongoCollectionEntry = getString("mongo.collection.entry")
  val MongoCollectionUser = getString("mongo.collection.user")
  val MongoCollectionMessage = getString("mongo.collection.message")
  val MongoCollectionHistory = getString("mongo.collection.history")
  val MongoCollectionRoom = getString("mongo.collection.room")
  val MongoCollectionWatcherRoom = getString("mongo.collection.watcher_room")
  val MongoCollectionConfig = getString("mongo.collection.config")
  val MongoCollectionCache = getString("mongo.collection.cache")
  val MongoCollectionSecurity = getString("mongo.collection.security")
  val MongoCollectionForumCateg = getString("mongo.collection.forum_categ")
  val MongoCollectionForumTopic = getString("mongo.collection.forum_topic")
  val MongoCollectionForumPost = getString("mongo.collection.forum_post")
  val MongoCollectionMessageThread = getString("mongo.collection.message_thread")
  val MongoCollectionWikiPage = getString("mongo.collection.wiki_page")
  val MongoCollectionFirewall = getString("mongo.collection.firewall")
  val MongoCollectionBookmark = getString("mongo.collection.bookmark")

  val FirewallCacheTtl = millis("firewall.cache_ttl")
  val FirewallEnabled = getBoolean("firewall.enabled")

  val ActorReporting = "reporting"
  val ActorSiteHub = "site_hub"
  val ActorGameHubMaster = "game_hub_master"
  val ActorLobbyHub = "lobby_hub"
  val ActorMonitorHub = "monitor_hub"

  private def millis(name: String): Int = getMilliseconds(name).toInt

  private def seconds(name: String): Int = millis(name) / 1000
}
