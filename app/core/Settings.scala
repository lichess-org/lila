package lila
package core

import com.typesafe.config.Config

final class Settings(config: Config) {

  import config._

  val SiteUidTimeout = millis("site.uid.timeout")

  val GameMessageLifetime = millis("game.message.lifetime")
  val GameUidTimeout = millis("game.uid.timeout")
  val GameHubTimeout = millis("game.hub.timeout")
  val GamePlayerTimeout = millis("game.player.timeout")
  val GameAnimationDelay = millis("game.animation.delay")
  val GameCachedNbTtl = millis("game.cached.nb.ttl")
  val GamePaginatorMaxPerPage = getInt("game.paginator.max_per_page")

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
  val MongoCollectionConfig = getString("mongo.collection.config")

  val ActorReporting = "reporting"
  val ActorSiteHub = "site_hub"
  val ActorGameHubMaster = "game_hub_master"
  val ActorLobbyHub = "lobby_hub"

  private def millis(name: String): Int = getMilliseconds(name).toInt

  private def seconds(name: String): Int = millis(name) / 1000
}
