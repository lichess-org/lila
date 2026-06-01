package lila.search

import alleycats.Zero
import play.api.libs.json.{ Json as PlayJson, * }
import scalalib.Json.given
import scalalib.paginator.Paginator
import scala.concurrent.Future

// this is probably misplaced, but i figure we should start using Long for offset/length somewhere.
// the blast radius of changing scalalib paginator would be quite large.

trait SearchAdapter[A]:

  def nbResults: Fu[Long]

  def slice(offset: Long, length: Long): Fu[Seq[A]]

  def map[B](f: A => B)(using Executor): SearchAdapter[B] = new:

    def nbResults = SearchAdapter.this.nbResults

    def slice(offset: Long, length: Long) =
      SearchAdapter.this.slice(offset, length).map(_.map(f))

  def mapFutureList[B](f: Seq[A] => Fu[Seq[B]])(using Executor): SearchAdapter[B] = new:

    def nbResults = SearchAdapter.this.nbResults

    def slice(offset: Long, length: Long) =
      SearchAdapter.this.slice(offset, length).flatMap(f)

class SearchPaginator[A] private[search] (
    val currentPage: Long,
    val maxPerPage: MaxPerPage,
    val currentPageResults: Seq[A],
    val nbResults: Long
):

  def previousPage: Option[Long] = Option.when(currentPage > 1)(currentPage - 1)

  def nextPage: Option[Long] =
    Option.when(currentPage < nbPages && currentPageResults.nonEmpty && currentPage < Long.MaxValue)(
      currentPage + 1
    )

  def nbPages: Long = SearchPaginator.nbPages(nbResults, maxPerPage)

  def hasToPaginate: Boolean = nbResults > maxPerPage.value.toLong

  def hasPreviousPage: Boolean = previousPage.isDefined

  def hasNextPage: Boolean = nextPage.isDefined

  def withCurrentPageResults[B](newResults: Seq[B]): SearchPaginator[B] =
    new SearchPaginator(
      currentPage = currentPage,
      maxPerPage = maxPerPage,
      currentPageResults = newResults,
      nbResults = nbResults
    )

  def mapResults[B](f: A => B): SearchPaginator[B] =
    withCurrentPageResults(currentPageResults.map(f))

  def mapList[B](f: Seq[A] => Seq[B]): SearchPaginator[B] =
    withCurrentPageResults(f(currentPageResults))

  def mapFutureResults[B](f: A => Fu[B])(using Executor): Fu[SearchPaginator[B]] =
    Future.traverse(currentPageResults)(f).dmap(withCurrentPageResults)

  def mapFutureList[B](f: Seq[A] => Fu[Seq[B]]): Fu[SearchPaginator[B]] =
    f(currentPageResults).dmap(withCurrentPageResults)

  def toPaginator: Paginator[A] =
    Paginator.fromResults(
      currentPageResults = currentPageResults,
      nbResults = math.min(nbResults, Int.MaxValue.toLong).toInt,
      currentPage = math.min(currentPage, Int.MaxValue.toLong).toInt,
      maxPerPage = maxPerPage
    )

object SearchPaginator:

  def apply[A](
      adapter: SearchAdapter[A],
      currentPage: Long,
      maxPerPage: MaxPerPage
  )(using Executor): Fu[SearchPaginator[A]] =
    if currentPage < 1 then apply(adapter, 1, maxPerPage)
    else if maxPerPage.value <= 0 then fuccess(empty[A])
    else
      for
        nbResults <- adapter.nbResults
        maxPage = nbPages(nbResults, maxPerPage)
        safePage = if maxPage <= 0 then 1 else math.min(currentPage.toLong, maxPage).toInt
        offset = (safePage.toLong - 1) * maxPerPage.value.toLong
        results <- adapter.slice(offset, maxPerPage.value.toLong)
      yield new SearchPaginator(safePage, maxPerPage, results, nbResults)

  def empty[A]: SearchPaginator[A] = new SearchPaginator(0, MaxPerPage(0), Nil, 0L)

  given [A] => Zero[SearchPaginator[A]]:
    def zero = empty[A]

  private def nbPages(nbResults: Long, maxPerPage: MaxPerPage): Long =
    if nbResults <= 0 || maxPerPage.value <= 0 then 0L
    else ((nbResults - 1) / maxPerPage.value.toLong) + 1

  given [A: Writes] => OWrites[SearchPaginator[A]] = OWrites[SearchPaginator[A]]: p =>
    PlayJson.obj(
      "currentPage" -> p.currentPage,
      "maxPerPage" -> p.maxPerPage,
      "currentPageResults" -> p.currentPageResults,
      "previousPage" -> p.previousPage,
      "nextPage" -> p.nextPage,
      "nbResults" -> p.nbResults,
      "nbPages" -> p.nbPages
    )

class PaginatorBuilder[A, Q](
    searchApi: SearchApi[A, Q],
    maxPerPage: MaxPerPage
)(using Executor):

  def apply(query: Q, page: Long): Fu[SearchPaginator[A]] =
    SearchPaginator(
      adapter = new SearchAdapter[A]:
        def nbResults = searchApi.count(query)
        def slice(offset: Long, length: Long) = searchApi.search(query, offset, length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )
