package lila.common
package paginator

import cats.data.Validated

import lila.common.config.MaxPerPage

final class Paginator[A] private[paginator] (
    val currentPage: Int,
    val maxPerPage: MaxPerPage,
    /** Returns the results for the current page.
      * The result is cached.
      */
    val currentPageResults: Seq[A],
    /** Returns the number of results.
      * The result is cached.
      */
    val nbResults: Int,
    val index: Int = 0
    /** we don't really need currentPage anymore as it should always be
      * index / config.postMaxPerPage.value + 1
      * leaving it in for compatibility
      * */
) {

  /** Returns the previous page.
    */
  def previousPage: Option[Int] = (currentPage > 1) option (currentPage - 1)

  /** Returns the next page.
    */
  def nextPage: Option[Int] =
    (currentPage < nbPages && currentPageResults.nonEmpty) option (currentPage + 1)

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
      nbResults = nbResults,
      index = index
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
      maxPerPage: MaxPerPage,
      index: Int = 0
  )(implicit ec: scala.concurrent.ExecutionContext): Fu[Paginator[A]] =
    validate(adapter, currentPage, maxPerPage, index) getOrElse apply(adapter, 1, maxPerPage, index)

  def empty[A]: Paginator[A] = new Paginator(0, MaxPerPage(0), Nil, 0, 0)

  implicit def zero[A] = ornicar.scalalib.Zero.instance(empty[A])

  def fromResults[A](
      currentPageResults: Seq[A],
      nbResults: Int,
      currentPage: Int,
      maxPerPage: MaxPerPage,
      index: Int = 0
  ): Paginator[A] =
    new Paginator(
      currentPage = currentPage,
      maxPerPage = maxPerPage,
      currentPageResults = currentPageResults,
      nbResults = nbResults,
      index = index
    )

  def validate[A](
      adapter: AdapterLike[A],
      currentPage: Int = 1,
      maxPerPage: MaxPerPage = MaxPerPage(10),
      index: Int = 1
  )(implicit ec: scala.concurrent.ExecutionContext): Validated[String, Fu[Paginator[A]]] = {
    if (currentPage < 1) Validated.invalid("Max per page must be greater than zero")
    else if (maxPerPage.value <= 0) Validated.invalid("Current page must be greater than zero")
    //else if (index < 1) Validated.invalid("Index must be greater than zero")
    else
      Validated.valid(for {
        results <- adapter.slice((currentPage - 1) * maxPerPage.value, maxPerPage.value)
        nbResults <- adapter.nbResults
      } yield new Paginator(currentPage, maxPerPage, results, nbResults, index))
  }
}
