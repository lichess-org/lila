package lidraughts.studySearch

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.common.paginator._
import lidraughts.hub.actorApi.study.RemoveStudy
import lidraughts.hub.LateMultiThrottler
import lidraughts.search._
import lidraughts.study.Study
import lidraughts.user.User

final class Env(
    config: Config,
    studyEnv: lidraughts.study.Env,
    makeClient: Index => ESClient,
    system: ActorSystem
) {

  private val IndexName = config getString "index"
  private val MaxPerPage = config getInt "paginator.max_per_page"

  private val client = makeClient(Index(IndexName))

  private val indexThrottler = system.actorOf(Props(new LateMultiThrottler(
    executionTimeout = 3.seconds.some,
    logger = logger
  )))

  val api = new StudySearchApi(
    client = client,
    indexThrottler = indexThrottler,
    studyEnv.studyRepo,
    studyEnv.chapterRepo
  )

  def apply(me: Option[User])(text: String, page: Int) =
    Paginator[Study.WithChaptersAndLiked](
      adapter = new AdapterLike[Study] {
        def query = Query(text, me.map(_.id))
        def nbResults = api count query
        def slice(offset: Int, length: Int) = api.search(query, From(offset), Size(length))
      } mapFutureList {
        studyEnv.pager.withChapters(_, Study.maxChapters)
      } mapFutureList studyEnv.pager.withLiking(me),
      currentPage = page,
      maxPerPage = lidraughts.common.MaxPerPage(MaxPerPage)
    )

  def cli = new lidraughts.common.Cli {
    def process = {
      case "study" :: "search" :: "reset" :: Nil => api.reset("reset", system) inject "done"
      case "study" :: "search" :: "index" :: since :: Nil => api.reset(since, system) inject "done"
    }
  }

  system.lidraughtsBus.subscribeFun('study) {
    case lidraughts.study.actorApi.SaveStudy(study) => api store study
    case RemoveStudy(id, _) => client deleteById Id(id)
  }
}

object Env {

  lazy val current = "studySearch" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "studySearch",
    studyEnv = lidraughts.study.Env.current,
    makeClient = lidraughts.search.Env.current.makeClient,
    system = lidraughts.common.PlayApp.system
  )
}
