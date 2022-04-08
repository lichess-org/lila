package lila.common
package paginator

import cats.data.Validated

import lila.common.config.MaxPerPage

final class Paginator[A] private[paginator] (
    val currentPage: Int,
    /** So there's some new functionality here expressed through negative currentPage values
      * that is only relevant to a few instances declared in ForumPaginator for infinite scroll.
      * Basically it is used to request ALL results "up to" a specific page in some uris and redirects
      * For example lichess.org/forum/blah/page=3 will fetch results 21-30 whereas page=-3
      * will fetch results 1-30.  I think this has maybe a 5% shot of surviving review but it's worth a shot
      * to avoid introducing a new request parameter, currently values less than -40 are considered
      * invalid.  NOTE:  This has no effect on any existing uses of paginator outside of ForumPaginator.
      */
    val maxPerPage: MaxPerPage,
    /** Returns the results for the current page.
      * The result is cached.
      */
    val currentPageResults: Seq[A],
    /** Returns the number of results.
      * The result is cached.
      */
    val nbResults: Int
) {

  /** Returns the previous page.
    */
  def previousPage: Option[Int] = {
    val page = if (currentPage < 0) 0 - currentPage else currentPage
    (page > 1) option (page - 1)
  }

  /** Returns the next page.
    */
  def nextPage: Option[Int] = {
    val page = if (currentPage < 0) 0 - currentPage else currentPage
    (page < nbPages && currentPageResults.nonEmpty) option (page + 1)
  }

  /** Returns the number of pages.
    */
  def nbPages: Int =
    if (maxPerPage.value > 0) (nbResults + maxPerPage.value - 1) / maxPerPage.value
    else 0

  /** Returns whether we have to paginate or not.
    * This is true if the number of results is higher than the max per page.
    */
  def hasToPaginate: Boolean = nbResults > maxPerPage.value

  /** Returns whether there is previous page or not.
    */
  def hasPreviousPage: Boolean = previousPage.isDefined

  /** Returns whether there is next page or not.
    */
  def hasNextPage: Boolean = nextPage.isDefined

  def withCurrentPageResults[B](newResults: Seq[B]): Paginator[B] =
    new Paginator(
      currentPage = currentPage,
      maxPerPage = maxPerPage,
      currentPageResults = newResults,
      nbResults = nbResults
    )

  def mapResults[B](f: A => B): Paginator[B] =
    withCurrentPageResults(currentPageResults map f)

  def mapFutureResults[B](f: A => Fu[B])(implicit ec: scala.concurrent.ExecutionContext): Fu[Paginator[B]] =
    currentPageResults.map(f).sequenceFu dmap withCurrentPageResults
}

object Paginator {

  def apply[A](
      adapter: AdapterLike[A],
      currentPage: Int,
      maxPerPage: MaxPerPage
  )(implicit ec: scala.concurrent.ExecutionContext): Fu[Paginator[A]] =
    validate(adapter, currentPage, maxPerPage) getOrElse apply(adapter, 1, maxPerPage)

  def empty[A]: Paginator[A] = new Paginator(0, MaxPerPage(0), Nil, 0)

  implicit def zero[A] = ornicar.scalalib.Zero.instance(empty[A])

  def fromResults[A](
      currentPageResults: Seq[A],
      nbResults: Int,
      currentPage: Int,
      maxPerPage: MaxPerPage
  ): Paginator[A] =
    new Paginator(
      currentPage = currentPage,
      maxPerPage = maxPerPage,
      currentPageResults = currentPageResults,
      nbResults = nbResults
    )

  def validate[A](
      adapter: AdapterLike[A],
      currentPage: Int = 1,
      maxPerPage: MaxPerPage = MaxPerPage(10)
  )(implicit ec: scala.concurrent.ExecutionContext): Validated[String, Fu[Paginator[A]]] = {
    if (currentPage < -40 || currentPage == 0)
      Validated.invalid("Current page is invalid") // see comment line 10'ish
    else if (maxPerPage.value <= 0) Validated.invalid("Max per page must be greater than zero")
    else
      Validated.valid(for {
        results   <- adapter.slice((currentPage - 1) * maxPerPage.value, maxPerPage.value)
        nbResults <- adapter.nbResults
      } yield new Paginator(currentPage, maxPerPage, results, nbResults))
  }
}
