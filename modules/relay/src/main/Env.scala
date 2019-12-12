package lila.relay

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    studyEnv: lila.study.Env,
    asyncCache: lila.memo.AsyncCache.Builder,
    slackApi: lila.slack.SlackApi,
    system: ActorSystem
) {

  private val MaxPerPage = config getInt "paginator.max_per_page"
  private val UserAgent = config getString "useragent"

  private val coll = db(config getString "collection.relay")

  lazy val forms = RelayForm

  private val repo = new RelayRepo(coll)

  private val withStudy = new RelayWithStudy(studyEnv.api)

  val jsonView = new JsonView(new RelayMarkup)

  val api = new RelayApi(
    repo = repo,
    studyApi = studyEnv.api,
    withStudy = withStudy,
    jsonView = jsonView,
    clearFormatCache = formatApi.refresh,
    system = system
  )

  lazy val pager = new RelayPager(
    repo = repo,
    withStudy = withStudy,
    maxPerPage = lila.common.MaxPerPage(MaxPerPage)
  )

  private val sync = new RelaySync(
    studyApi = studyEnv.api,
    chapterRepo = studyEnv.chapterRepo
  )

  private lazy val formatApi = new RelayFormatApi(asyncCache, UserAgent)

  system.actorOf(Props(new RelayFetch(
    sync = sync,
    api = api,
    slackApi = slackApi,
    formatApi = formatApi,
    chapterRepo = studyEnv.chapterRepo,
    userAgent = UserAgent
  )))

  system.scheduler.schedule(1 minute, 1 minute) {
    api.autoStart >> api.autoFinishNotSyncing
  }

  lila.common.Bus.subscribeFun('studyLikes, 'study, 'relayToggle) {
    case lila.study.actorApi.StudyLikes(id, likes) => api.setLikes(Relay.Id(id.value), likes)
    case lila.hub.actorApi.study.RemoveStudy(studyId, _) => api.onStudyRemove(studyId)
    case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyEnv.api.isContributor(id, who.u) flatMap {
        _ ?? {
          api.requestPlay(Relay.Id(id.value), v)
        }
      }
  }
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "relay",
    studyEnv = lila.study.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache,
    slackApi = lila.slack.Env.current.api,
    system = lila.common.PlayApp.system
  )
}
