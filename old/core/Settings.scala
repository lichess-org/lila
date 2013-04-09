package lila.app
package core

import com.typesafe.config.Config
import scalaz.{ Success, Failure }
import scala.collection.JavaConversions._

final class Settings(config: Config, val IsDev: Boolean) {

  import config._

  val ConfigName = getString("config_name")

  val SiteUidTimeout = millis("site.uid.timeout")

  val MonitorTimeout = millis("monitor.timeout")

  val I18nWebPathRelative = getString("i18n.web_path.relative")
  val I18nFilePathRelative = getString("i18n.file_path.relative")
  val I18nUpstreamDomain = getString("i18n.upstream.domain")
  val I18nHideCallsCookieName = getString("i18n.hide_calls.cookie.name")
  val I18nHideCallsCookieMaxAge = getInt("i18n.hide_calls.cookie.max_age")
  val I18nCollectionTranslation = getString("i18n.collection.translation")

  val GameCachedNbTtl = millis("game.cached.nb.ttl")
  val GamePaginatorMaxPerPage = getInt("game.paginator.max_per_page")
  val GameCollectionGame = getString("game.collection.game")
  val GameCollectionPgn = getString("game.collection.pgn")
  val GameJsPathRaw = getString("game.js_path.raw")
  val GameJsPathCompiled = getString("game.js_path.compiled")

  val SearchESHost = getString("search.elasticsearch.host")
  val SearchESPort = getInt("search.elasticsearch.port")
  val SearchESCluster = getString("search.elasticsearch.cluster")
  val SearchPaginatorMaxPerPage = getInt("search.paginator.max_per_page")
  val SearchCollectionQueue = getString("search.collection.queue")


  val TournamentCollectionTournament = getString("tournament.collection.tournament")
  val TournamentCollectionRoom = getString("tournament.collection.room")
  val TournamentMessageLifetime = millis("tournament.message.lifetime")
  val TournamentUidTimeout = millis("tournament.uid.timeout")
  val TournamentHubTimeout = millis("tournament.hub.timeout")
  val TournamentMemoTtl = millis("tournament.memo.ttl")

  val AnalyseCachedNbTtl = millis("analyse.cached.nb.ttl")

  val UserPaginatorMaxPerPage = getInt("user.paginator.max_per_page")
  val UserEloUpdaterFloor = getInt("user.elo_updater.floor")
  val UserCachedNbTtl = millis("user.cached.nb.ttl")
  val UserCollectionUser = getString("user.collection.user")
  val UserCollectionHistory = getString("user.collection.history")

  val ForumTopicMaxPerPage = getInt("forum.topic.max_per_page")
  val ForumPostMaxPerPage = getInt("forum.post.max_per_page")
  val ForumSearchMaxPerPage = getInt("forum.search.max_per_page")
  val ForumRecentTimeout = millis("forum.recent.timeout")
  val ForumCollectionCateg = getString("forum.collection.categ")
  val ForumCollectionTopic = getString("forum.collection.topic")
  val ForumCollectionPost = getString("forum.collection.post")

  val MessageThreadMaxPerPage = getInt("message.thread.max_per_page")


  val MemoHookTimeout = millis("memo.hook.timeout")
  val MemoUsernameTimeout = millis("memo.username.timeout")

  val FinisherLockTimeout = millis("memo.finisher_lock.timeout")

  sealed trait AiEngine
  case object AiStockfish extends AiEngine
  case object AiStupid extends AiEngine

  val AiChoice: AiEngine = getString("ai.use") match {
    case "stockfish" ⇒ AiStockfish
    case _           ⇒ AiStupid
  }

  val AiServerMode = getBoolean("ai.server")
  val AiClientMode = getBoolean("ai.client")

  val AiStockfishExecPath = getString("ai.stockfish.exec_path")
  val AiStockfishHashSize = getInt("ai.stockfish.hash_size")
  val AiStockfishThreads = getInt("ai.stockfish.threads")
  val AiStockfishPlayUrl = getString("ai.stockfish.play.url")
  val AiStockfishPlayMaxMoveTime = getInt("ai.stockfish.play.movetime")
  val AiStockfishAnalyseUrl = getString("ai.stockfish.analyse.url")
  val AiStockfishAnalyseMoveTime = getInt("ai.stockfish.analyse.movetime")
  val AiStockfishDebug = getBoolean("ai.stockfish.debug")

  val MongoHost = getString("mongo.host")
  val MongoPort = getInt("mongo.port")
  val MongoDbName = getString("mongo.dbName")
  val MongoConnectionsPerHost = getInt("mongo.connectionsPerHost")
  val MongoAutoConnectRetry = getBoolean("mongo.autoConnectRetry")
  val MongoConnectTimeout = millis("mongo.connectTimeout")
  val MongoBlockingThreads = getInt("mongo.threadsAllowedToBlockForConnectionMultiplier")

  val AnalyseCollectionAnalysis = getString("analyse.collection.analysis")

  val FirewallEnabled = getBoolean("firewall.enabled")
  val FirewallCookieEnabled = getBoolean("firewall.cookie.enabled")
  val FirewallCookieName = getString("firewall.cookie.name")
  val FirewallCollectionFirewall = getString("firewall.collection.firewall")

  val MessageCollectionThread = getString("message.collection.thread")

  val WikiCollectionPage = getString("wiki.collection.page")
  val WikiGitUrl = getString("wiki.git_url")

  val TeamCollectionTeam = getString("team.collection.team")
  val TeamCollectionMember = getString("team.collection.member")
  val TeamCollectionRequest = getString("team.collection.request")
  val TeamPaginatorMaxPerPage = getInt("team.paginator.max_per_page")
  val TeamPaginatorMaxUserPerPage = getInt("team.paginator.max_user_per_page")

  val BookmarkCollectionBookmark = getString("bookmark.collection.bookmark")

  val CoreCollectionCache = getString("core.collection.cache")
  val CoreCronEnabled = getBoolean("core.cron.enabled")

  val CliUsername = getString("cli.username")

  val NetDomain = getString("net.domain")
  val NetBaseUrl = getString("net.base_url")

  val SecurityCollectionSecurity = getString("security.collection.security")
  val SecurityWiretapIps = getStringList("security.wiretap.ips").toList

  val ActorReporting = "reporting"
  val ActorSiteHub = "site_hub"
  val ActorRoundHubMaster = "game_hub_master"
  val ActorLobbyHub = "lobby_hub"
  val ActorMonitorHub = "monitor_hub"
  val ActorTournamentHubMaster = "tournament_hub_master"
  val ActorTournamentOrganizer = "tournament_organizer"
  val ActorTournamentReminder = "tournament_reminder"
  val ActorTournamentRegister = "tournament_register"

  val ModlogCollectionModlog = getString("modlog.collection.modlog")

  private def millis(name: String): Int = getMilliseconds(name).toInt

  private def seconds(name: String): Int = millis(name) / 1000

  implicit def validAny[A](a: A) = new {
    def valid(f: A ⇒ Valid[A]): A = f(a) match {
      case Success(a)   ⇒ a
      case Failure(err) ⇒ throw new Invalid(err.shows)
    }
  }

  private class Invalid(msg: String) extends Exception(msg)
}
