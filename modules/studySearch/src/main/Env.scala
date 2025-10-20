package lila.studySearch

import com.softwaremill.macwire.*
import scalalib.paginator.*

import lila.search.*
import lila.search.client.SearchClient
import lila.search.spec.{ Query, StudySorting }
import lila.study.Study
import lila.search.spec.{ Order as SpecOrder, StudySortField }
import lila.core.study.Order

final class Env(
    studyRepo: lila.study.StudyRepo,
    pager: lila.study.StudyPager,
    client: SearchClient
)(using Executor):

  val api: StudySearchApi = wire[StudySearchApi]

  def apply(text: String, order: Order, page: Int)(using me: Option[Me]) =
    Paginator[Study.WithChaptersAndLiked](
      adapter = new AdapterLike[Study]:
        def query =
          Query.study(
            text.take(100),
            order.toSpec,
            me.map(_.userId.value)
          )
        def nbResults = api.count(query).dmap(_.toInt)
        def slice(offset: Int, length: Int) = api.search(query, From(offset), Size(length))
      .mapFutureList(pager.withChaptersAndLiking()),
      currentPage = page,
      maxPerPage = pager.maxPerPage
    )

  extension (x: Order)
    def toSpec: Option[StudySorting] =
      x.match
        case Order.alphabetical => StudySorting(StudySortField.Name, SpecOrder.Asc).some
        case Order.hot => StudySorting(StudySortField.Hot, SpecOrder.Desc).some
        case Order.newest => StudySorting(StudySortField.CreatedAt, SpecOrder.Desc).some
        case Order.oldest => StudySorting(StudySortField.CreatedAt, SpecOrder.Asc).some
        case Order.popular => StudySorting(StudySortField.Likes, SpecOrder.Desc).some
        case Order.updated => StudySorting(StudySortField.UpdatedAt, SpecOrder.Desc).some
        case Order.mine => StudySorting(StudySortField.Likes, SpecOrder.Asc).some
        case Order.relevant => none
