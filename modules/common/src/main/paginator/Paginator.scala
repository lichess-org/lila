package lila.common
package paginator

import scalaz.Success

final class Paginator[A] private[paginator] (
    val currentPage: Int,
    val maxPerPage: Int,
    /**
     * Returns the results for the current page.
     * The result is cached.
     */
    val currentPageResults: Seq[A],
    /**
     * Returns the number of results.
     * The result is cached.
     */
    val nbResults: Int
) {

  /**
   * Returns the previous page.
   */
  def previousPage: Option[Int] = (currentPage > 1) option (currentPage - 1)

  /**
   * Returns the next page.
   */
  def nextPage: Option[Int] = (currentPage < nbPages) option (currentPage + 1)

  /**
   * Returns the number of pages.
   */
  def nbPages: Int = scala.math.ceil(nbResults.toFloat / maxPerPage).toInt

  /**
   * Returns whether we have to paginate or not.
   * This is true if the number of results is higher than the max per page.
   */
  def hasToPaginate: Boolean = nbResults > maxPerPage

  /**
   * Returns whether there is previous page or not.
   */
  def hasPreviousPage: Boolean = previousPage.isDefined

  /**
   * Returns whether there is next page or not.
   */
  def hasNextPage: Boolean = nextPage.isDefined

  def withCurrentPageResults[B](newResults: Seq[B]): Paginator[B] = new Paginator(
    currentPage = currentPage,
    maxPerPage = maxPerPage,
    currentPageResults = newResults,
    nbResults = nbResults
  )

  def mapResults[B](f: A => B): Paginator[B] =
    withCurrentPageResults(currentPageResults map f)
}

object Paginator {

  def apply[A](
    adapter: AdapterLike[A],
    currentPage: Int = 1,
    maxPerPage: Int = 10
  ): Fu[Paginator[A]] =
    validate(adapter, currentPage, maxPerPage) | apply(adapter, 1, maxPerPage)

  def empty[A]: Paginator[A] = new Paginator(0, 0, Nil, 0)

  def fromList[A](
    list: List[A],
    currentPage: Int = 1,
    maxPerPage: Int = 10
  ): Paginator[A] = new Paginator(
    currentPage = currentPage,
    maxPerPage = maxPerPage,
    currentPageResults = list.drop((currentPage - 1) * maxPerPage).take(maxPerPage),
    nbResults = list.size
  )

  def validate[A](
    adapter: AdapterLike[A],
    currentPage: Int = 1,
    maxPerPage: Int = 10
  ): Valid[Fu[Paginator[A]]] =
    if (currentPage < 1) !!("Max per page must be greater than zero")
    else if (maxPerPage <= 0) !!("Current page must be greater than zero")
    else Success(for {
      results ← adapter.slice((currentPage - 1) * maxPerPage, maxPerPage)
      nbResults ← adapter.nbResults
    } yield new Paginator(currentPage, maxPerPage, results, nbResults))
}
