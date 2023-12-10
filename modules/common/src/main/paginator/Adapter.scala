package lila.common
package paginator

abstract class AdapterLike[A](using Executor):

  /** Returns the total number of results.
    */
  def nbResults: Fu[Int]

  /** Returns a slice of the results.
    *
    * @param offset
    *   The number of elements to skip, starting from zero
    * @param length
    *   The maximum number of elements to return
    */
  def slice(offset: Int, length: Int): Fu[Seq[A]]

  def map[B](f: A => B): AdapterLike[B] = new:

    def nbResults = AdapterLike.this.nbResults

    def slice(offset: Int, length: Int) =
      AdapterLike.this.slice(offset, length) dmap { _ map f }

  def mapFutureList[B](f: Seq[A] => Fu[Seq[B]]): AdapterLike[B] = new:

    def nbResults = AdapterLike.this.nbResults

    def slice(offset: Int, length: Int) =
      AdapterLike.this.slice(offset, length) flatMap f
