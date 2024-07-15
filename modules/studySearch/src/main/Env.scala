package lila.studySearch

import akka.actor.*
import com.softwaremill.macwire.*
import scalalib.paginator.*

import lila.common.{ Bus, LateMultiThrottler }
import lila.core.study.RemoveStudy
import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.Query
import lila.study.Study

final class Env(
    studyRepo: lila.study.StudyRepo,
    chapterRepo: lila.study.ChapterRepo,
    pager: lila.study.StudyPager,
    client: SearchClient
)(using Executor, ActorSystem, Scheduler, akka.stream.Materializer):

  private val indexThrottler = LateMultiThrottler(executionTimeout = 3.seconds.some, logger = logger)

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(me: Option[User])(text: String, page: Int) =
    Paginator[Study.WithChaptersAndLiked](
      adapter = new AdapterLike[Study]:
        def query                           = Query.study(text.take(100), me.map(_.id.value))
        def nbResults                       = api.count(query).dmap(_.toInt)
        def slice(offset: Int, length: Int) = api.search(query, From(offset), Size(length))
      .mapFutureList(pager.withChaptersAndLiking(me)),
      currentPage = page,
      maxPerPage = pager.maxPerPage
    )

  def cli = new lila.common.Cli:
    def process =
      case "study" :: "search" :: "reset" :: Nil          => api.reset("reset").inject("done")
      case "study" :: "search" :: "index" :: since :: Nil => api.reset(since).inject("done")

  Bus.subscribeFun("study"):
    case lila.study.actorApi.SaveStudy(study) => api.store(study)
    case RemoveStudy(id)                      => client.deleteById(index, id.value)
