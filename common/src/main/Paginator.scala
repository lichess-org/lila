package lila.common
package paginator

import scalaz.Success

import play.api.libs.concurrent.Execution.Implicits._

trait AdapterLike[A] {

  /**
   * Returns the total number of results.
   */
  def nbResults: Fu[Int]

  /**
   * Returns a slice of the results.
   *
   * @param   offset    The number of elements to skip, starting from zero
   * @param   length    The maximum number of elements to return
   */
  def slice(offset: Int, length: Int): Fu[Seq[A]]

  /**
   * FUNCTOR INTERFACE
   */
  def map[B](f: A ⇒ B): AdapterLike[B] = new AdapterLike[B] {
    def nbResults = AdapterLike.this.nbResults
    def slice(offset: Int, length: Int) = AdapterLike.this.slice(offset, length) map2 f
  }
}

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
   * FUNCTOR INTERFACE
   */

  def map[B](f: A ⇒ B): Paginator[B] = new Paginator(
    currentPage = currentPage,
    maxPerPage = maxPerPage,
    currentPageResults = currentPageResults map f,
    nbResults = nbResults)

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
    maxPerPage: Int = 10): Valid[Fu[Paginator[A]]] =
    if (currentPage <= 0) !!("Max per page must be greater than zero")
    else if (maxPerPage <= 0) !!("Current page must be greater than zero")
    else Success(for {
      results ← adapter.slice((currentPage - 1) * maxPerPage, maxPerPage)
      nbResults ← adapter.nbResults
    } yield new Paginator(currentPage, maxPerPage, results, nbResults))
}
