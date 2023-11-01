package lila.studySearch

import akka.actor.*
import com.softwaremill.macwire.*

import lila.common.Bus
import lila.common.paginator.*
import lila.hub.actorApi.study.RemoveStudy
import lila.hub.LateMultiThrottler
import lila.search.*
import lila.study.Study
import lila.user.User

final class Env(
    studyRepo: lila.study.StudyRepo,
    chapterRepo: lila.study.ChapterRepo,
    pager: lila.study.StudyPager,
    makeClient: Index => ESClient
)(using Executor, ActorSystem, Scheduler, akka.stream.Materializer):

  private val client = makeClient(Index("study"))

  private val indexThrottler = LateMultiThrottler(executionTimeout = 3.seconds.some, logger = logger)

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(me: Option[User])(text: String, page: Int) =
    Paginator[Study.WithChaptersAndLiked](
      adapter = new AdapterLike[Study]:
        def query                           = Query(text take 100, me.map(_.id))
        def nbResults                       = api count query
        def slice(offset: Int, length: Int) = api.search(query, From(offset), Size(length))
      .mapFutureList(pager.withChaptersAndLiking(me)),
      currentPage = page,
      maxPerPage = pager.maxPerPage
    )

  def cli = new lila.common.Cli:
    def process =
      case "study" :: "search" :: "reset" :: Nil          => api.reset("reset") inject "done"
      case "study" :: "search" :: "index" :: since :: Nil => api.reset(since) inject "done"

  Bus.subscribeFun("study"):
    case lila.study.actorApi.SaveStudy(study) => api.store(study)
    case RemoveStudy(id)                      => client.deleteById(id into Id)
