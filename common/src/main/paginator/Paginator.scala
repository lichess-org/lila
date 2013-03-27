package lila.common
package paginator

import scalaz.Success

import play.api.libs.concurrent.Execution.Implicits._

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
    val nbResults: Int) {

  /**
   * Returns the previous page.
   */
  def previousPage: Option[Int] =
    if (currentPage == 1) None else Some(currentPage - 1)

  /**
   * Returns the next page.
   */
  def nextPage: Option[Int] =
    if (currentPage == nbPages) None else Some(currentPage + 1)

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
  def hasPreviousPage: Boolean = None != previousPage

  /**
   * Returns whether there is next page or not.
   */
  def hasNextPage: Boolean = None != nextPage
}

object Paginator {

  def apply[A](
    adapter: AdapterLike[A],
    currentPage: Int = 1,
    maxPerPage: Int = 10): Fu[Paginator[A]] =
    validate(adapter, currentPage, maxPerPage).fold(
      _ ⇒ apply(adapter, 1, maxPerPage),
      identity
    )

  def validate[A](
    adapter: AdapterLike[A],
    currentPage: Int = 1,
    maxPerPage: Int = 10): Valid[Fu[Paginator[A]]] =
    if (currentPage < 1) !!("Max per page must be greater than zero")
    else if (maxPerPage <= 0) !!("Current page must be greater than zero")
    else Success(for {
      results ← adapter.slice((currentPage - 1) * maxPerPage, maxPerPage)
      nbResults ← adapter.nbResults
    } yield new Paginator(currentPage, maxPerPage, results, nbResults))
}
