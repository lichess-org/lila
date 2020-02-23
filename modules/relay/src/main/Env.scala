package lidraughts.relay

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    studyEnv: lidraughts.study.Env,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val MaxPerPage = config getInt "paginator.max_per_page"
  private val FeaturableCacheTtl = config duration "featurable.cache.ttl"

  private val coll = db(config getString "collection.relay")

  lazy val forms = RelayForm

  private val repo = new RelayRepo(coll)

  private val withStudy = new RelayWithStudy(studyEnv.api)

  val jsonView = new JsonView(new RelayMarkup)

  val api = new RelayApi(
    repo = repo,
    studyApi = studyEnv.api,
    socketMap = studyEnv.socketMap,
    withStudy = withStudy,
    jsonView = jsonView,
    clearFormatCache = formatApi.refresh,
    system = system
  )

  lazy val pager = new RelayPager(
    repo = repo,
    withStudy = withStudy,
    maxPerPage = lidraughts.common.MaxPerPage(MaxPerPage)
  )

  private val sync = new RelaySync(
    studyApi = studyEnv.api,
    chapterRepo = studyEnv.chapterRepo
  )

  lazy val socketHandler = new SocketHandler(
    studyHandler = studyEnv.socketHandler,
    api = api
  )

  val allFeaturable = asyncCache.single(
    name = "relay.allFeaturable",
    repo.featurable,
    expireAfter = _.ExpireAfterWrite(FeaturableCacheTtl)
  )

  private lazy val formatApi = new RelayFormatApi(asyncCache)

  system.actorOf(Props(new RelayFetch(
    sync = sync,
    api = api,
    formatApi = formatApi,
    chapterRepo = studyEnv.chapterRepo
  )))

  system.scheduler.schedule(1 minute, 1 minute) {
    api.autoStart >> api.autoFinishNotSyncing
  }

  system.lidraughtsBus.subscribeFun('studyLikes, 'study) {
    case lidraughts.study.actorApi.StudyLikes(id, likes) => api.setLikes(Relay.Id(id.value), likes)
    case lidraughts.hub.actorApi.study.RemoveStudy(studyId, _) => api.onStudyRemove(studyId)
  }
}

object Env {

  lazy val current: Env = "relay" boot new Env(
    db = lidraughts.db.Env.current,
    config = lidraughts.common.PlayApp loadConfig "relay",
    studyEnv = lidraughts.study.Env.current,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    system = lidraughts.common.PlayApp.system
  )
}
