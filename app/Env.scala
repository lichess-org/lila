package lidraughts.app

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.user.User

final class Env(
    config: Config,
    val scheduler: lidraughts.common.Scheduler,
    val system: ActorSystem,
    appPath: String
) {

  private val RendererName = config getString "app.renderer.name"

  lazy val preloader = new mashup.Preload(
    tv = Env.tv.tv,
    leaderboard = Env.user.cached.topWeek,
    tourneyWinners = Env.tournament.winners.all.map(_.top),
    timelineEntries = Env.timeline.entryApi.userEntries _,
    dailyPuzzle = tryDailyPuzzle,
    liveStreams = () => Env.streamer.liveStreamApi.all,
    countRounds = () => Env.round.count,
    lobbyApi = Env.api.lobbyApi,
    getPlayban = Env.playban.api.currentBan _,
    lightUserApi = Env.user.lightUserApi,
    roundProxyPov = Env.round.proxy.pov _,
    urgentGames = Env.round.proxy.urgentGames _
  )

  lazy val socialInfo = mashup.UserInfo.Social(
    relationApi = Env.relation.api,
    noteApi = Env.user.noteApi,
    prefApi = Env.pref.api
  ) _

  lazy val userNbGames = mashup.UserInfo.NbGames(
    crosstableApi = Env.game.crosstableApi,
    bookmarkApi = Env.bookmark.api,
    gameCached = Env.game.cached
  ) _

  lazy val userInfo = mashup.UserInfo(
    relationApi = Env.relation.api,
    trophyApi = Env.user.trophyApi,
    shieldApi = Env.tournament.shieldApi,
    revolutionApi = Env.tournament.revolutionApi,
    postApi = Env.forum.postApi,
    studyRepo = Env.study.studyRepo,
    getRatingChart = Env.history.ratingChartApi.apply,
    getRanks = Env.user.cached.rankingsOf,
    isHostingSimul = Env.simul.isHosting,
    fetchIsStreamer = Env.streamer.api.isStreamer,
    fetchTeamIds = Env.team.cached.teamIdsList,
    insightShare = Env.insight.share,
    getPlayTime = Env.game.playTime.apply,
    completionRate = Env.playban.api.completionRate
  ) _

  lazy val teamInfo = new mashup.TeamInfoApi(
    api = Env.team.api,
    getForumNbPosts = Env.forum.categApi.teamNbPosts _,
    getForumPosts = Env.forum.recent.team _,
    asyncCache = Env.memo.asyncCache,
    memberColl = Env.team.colls.member,
    userColl = Env.user.userColl
  )

  private val tryDailyPuzzle: lidraughts.puzzle.Daily.Try = () =>
    scala.concurrent.Future {
      Env.puzzle.daily.get
    }.flatMap(identity).withTimeoutDefault(50 millis, none)(system) recover {
      case e: Exception =>
        lidraughts.log("preloader").warn("daily puzzle", e)
        none
    }

  def closeAccount(userId: lidraughts.user.User.ID, self: Boolean): Funit = for {
    user <- lidraughts.user.UserRepo byId userId flatten s"No such user $userId"
    goodUser <- !user.lameOrTroll ?? { !Env.playban.api.hasCurrentBan(user.id) }
    _ <- lidraughts.user.UserRepo.disable(user, keepEmail = !goodUser)
    _ = Env.user.onlineUserIdMemo.remove(user.id)
    _ = Env.user.recentTitledUserIdMemo.remove(user.id)
    following <- Env.relation.api fetchFollowing user.id
    _ <- !goodUser ?? Env.activity.write.unfollowAll(user, following)
    _ <- Env.relation.api.unfollowAll(user.id)
    _ <- Env.user.rankingApi.remove(user.id)
    _ <- Env.team.api.quitAll(user.id)
    _ = Env.challenge.api.removeByUserId(user.id)
    _ = Env.tournament.api.withdrawAll(user)
    _ <- Env.plan.api.cancel(user).nevermind
    _ <- Env.lobby.seekApi.removeByUser(user)
    _ <- Env.security.store.disconnect(user.id)
    _ <- Env.push.webSubscriptionApi.unsubscribeByUser(user)
    _ <- Env.streamer.api.demote(user.id)
    //_ <- Env.coach.api.remove(user.id)
    reports <- Env.report.api.processAndGetBySuspect(lidraughts.report.Suspect(user))
    _ <- self ?? Env.mod.logApi.selfCloseAccount(user.id, reports)
  } yield {
    system.lidraughtsBus.publish(lidraughts.hub.actorApi.security.CloseAccount(user.id), 'accountClose)
  }

  system.lidraughtsBus.subscribeFun('garbageCollect, 'playban) {
    case lidraughts.hub.actorApi.security.GarbageCollect(userId, _) => kill(userId)
    case lidraughts.hub.actorApi.playban.SitcounterClose(userId) => kill(userId)
  }

  private def kill(userId: User.ID): Unit =
    system.scheduler.scheduleOnce(1 second) {
      closeAccount(userId, self = false)
    }

  system.actorOf(Props(new actor.Renderer), name = RendererName)

  lidraughts.common.Chronometer.syncEffect(List(
    Env.socket,
    Env.site,
    Env.tournament,
    Env.lobby,
    Env.game,
    Env.setup,
    Env.round,
    Env.team,
    Env.message,
    Env.timeline,
    Env.gameSearch,
    Env.teamSearch,
    Env.forumSearch,
    Env.relation,
    Env.report,
    Env.bookmark,
    Env.pref,
    Env.chat,
    Env.puzzle,
    Env.tv,
    Env.blog,
    Env.playban, // required to load the actor
    Env.shutup, // required to load the actor
    Env.insight, // required to load the actor
    Env.push, // required to load the actor
    Env.perfStat, // required to load the actor
    Env.slack, // required to load the actor
    Env.challenge, // required to load the actor
    Env.explorer, // required to load the actor
    Env.draughtsnet, // required to schedule the cleaner
    Env.notifyModule, // required to load the actor
    Env.plan, // required to load the actor
    Env.event, // required to load the actor
    Env.activity, // required to load the actor
    Env.relay // you know the drill by now
  )) { lap =>
    lidraughts.log.boot.info(s"${lap.millis}ms Preloading complete")
  }

  scheduler.once(5 seconds) { Env.slack.api.publishRestart }
  scheduler.once(10 seconds) {
    // delayed preloads
    Env.oAuth
    Env.studySearch
  }
}

object Env {

  lazy val current = "app" boot new Env(
    config = lidraughts.common.PlayApp.loadConfig,
    scheduler = lidraughts.common.PlayApp.scheduler,
    system = lidraughts.common.PlayApp.system,
    appPath = lidraughts.common.PlayApp withApp (_.path.getCanonicalPath)
  )

  def api = lidraughts.api.Env.current
  def db = lidraughts.db.Env.current
  def user = lidraughts.user.Env.current
  def security = lidraughts.security.Env.current
  def hub = lidraughts.hub.Env.current
  def socket = lidraughts.socket.Env.current
  def memo = lidraughts.memo.Env.current
  def message = lidraughts.message.Env.current
  def i18n = lidraughts.i18n.Env.current
  def game = lidraughts.game.Env.current
  def bookmark = lidraughts.bookmark.Env.current
  def search = lidraughts.search.Env.current
  def gameSearch = lidraughts.gameSearch.Env.current
  def timeline = lidraughts.timeline.Env.current
  def forum = lidraughts.forum.Env.current
  def forumSearch = lidraughts.forumSearch.Env.current
  def team = lidraughts.team.Env.current
  def teamSearch = lidraughts.teamSearch.Env.current
  def analyse = lidraughts.analyse.Env.current
  def mod = lidraughts.mod.Env.current
  def notifyModule = lidraughts.notify.Env.current
  def site = lidraughts.site.Env.current
  def round = lidraughts.round.Env.current
  def lobby = lidraughts.lobby.Env.current
  def setup = lidraughts.setup.Env.current
  def importer = lidraughts.importer.Env.current
  def tournament = lidraughts.tournament.Env.current
  def simul = lidraughts.simul.Env.current
  def relation = lidraughts.relation.Env.current
  def report = lidraughts.report.Env.current
  def pref = lidraughts.pref.Env.current
  def chat = lidraughts.chat.Env.current
  def puzzle = lidraughts.puzzle.Env.current
  def coordinate = lidraughts.coordinate.Env.current
  def tv = lidraughts.tv.Env.current
  def blog = lidraughts.blog.Env.current
  def history = lidraughts.history.Env.current
  def playban = lidraughts.playban.Env.current
  def shutup = lidraughts.shutup.Env.current
  def insight = lidraughts.insight.Env.current
  def push = lidraughts.push.Env.current
  def perfStat = lidraughts.perfStat.Env.current
  def slack = lidraughts.slack.Env.current
  def challenge = lidraughts.challenge.Env.current
  def explorer = lidraughts.explorer.Env.current
  def draughtsnet = lidraughts.draughtsnet.Env.current
  def study = lidraughts.study.Env.current
  def studySearch = lidraughts.studySearch.Env.current
  def learn = lidraughts.learn.Env.current
  def plan = lidraughts.plan.Env.current
  def event = lidraughts.event.Env.current
  def pool = lidraughts.pool.Env.current
  def practice = lidraughts.practice.Env.current
  def irwin = lidraughts.irwin.Env.current
  def activity = lidraughts.activity.Env.current
  def relay = lidraughts.relay.Env.current
  def streamer = lidraughts.streamer.Env.current
  def oAuth = lidraughts.oauth.Env.current
  def bot = lidraughts.bot.Env.current
  def evalCache = lidraughts.evalCache.Env.current
  def rating = lidraughts.rating.Env.current
}
