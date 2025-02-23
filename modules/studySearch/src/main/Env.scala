package lila.studySearch

import com.softwaremill.macwire.*
import scalalib.paginator.*

import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.Query
import lila.study.Study

final class Env(
    studyRepo: lila.study.StudyRepo,
    pager: lila.study.StudyPager,
    client: SearchClient
)(using Executor):

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(text: String, page: Int)(using me: Option[Me]) =
    Paginator[Study.WithChaptersAndLiked](
      adapter = new AdapterLike[Study]:
        def query                           = Query.study(text.take(100), me.map(_.userId.value))
        def nbResults                       = api.count(query).dmap(_.toInt)
        def slice(offset: Int, length: Int) = api.search(query, From(offset), Size(length))
      .mapFutureList(pager.withChaptersAndLiking()),
      currentPage = page,
      maxPerPage = pager.maxPerPage
    )
