package lila.studySearch

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config._
import lila.common.paginator._
import lila.hub.actorApi.study.RemoveStudy
import lila.hub.LateMultiThrottler
import lila.search._
import lila.study.Study
import lila.user.User

@Module
private class StudySearchConfig(
    @ConfigName("index.name") val indexName: String,
    @ConfigName("paginator.max_per_page") val maxPerPage: MaxPerPage
)

final class Env(
    appConfig: Configuration,
    studyRepo: lila.study.StudyRepo,
    chapterRepo: lila.study.ChapterRepo,
    pager: lila.study.StudyPager,
    makeClient: Index => ESClient
)(implicit system: ActorSystem, mat: akka.stream.Materializer) {

  private val config = appConfig.get[StudySearchConfig]("studySearch")(AutoConfig.loader)

  private val client = makeClient(Index(config.indexName))

  private val indexThrottler = system.actorOf(Props(new LateMultiThrottler(
    executionTimeout = 3.seconds.some,
    logger = logger
  )))

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(me: Option[User])(text: String, page: Int) =
    Paginator[Study.WithChaptersAndLiked](
      adapter = new AdapterLike[Study] {
        def query = Query(text, me.map(_.id))
        def nbResults = api count query
        def slice(offset: Int, length: Int) = api.search(query, From(offset), Size(length))
      } mapFutureList {
        pager.withChapters(_, Study.maxChapters)
      } mapFutureList pager.withLiking(me),
      currentPage = page,
      maxPerPage = config.maxPerPage
    )

  def cli = new lila.common.Cli {
    def process = {
      case "study" :: "search" :: "reset" :: Nil => api.reset("reset") inject "done"
      case "study" :: "search" :: "index" :: since :: Nil => api.reset(since) inject "done"
    }
  }

  Bus.subscribeFun("study") {
    case lila.study.actorApi.SaveStudy(study) => api store study
    case RemoveStudy(id, _) => client deleteById Id(id)
  }
}
