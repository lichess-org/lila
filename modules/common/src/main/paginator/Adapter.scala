package lila.common
package paginator

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
  def map[B](f: A => B): AdapterLike[B] = new AdapterLike[B] {

    def nbResults = AdapterLike.this.nbResults

    def slice(offset: Int, length: Int) =
      AdapterLike.this.slice(offset, length) map { _ map f }
  }

  def mapFuture[B](f: A => Fu[B]): AdapterLike[B] = new AdapterLike[B] {

    def nbResults = AdapterLike.this.nbResults

    def slice(offset: Int, length: Int) =
      AdapterLike.this.slice(offset, length) flatMap { results =>
        results.map(f).sequenceFu
      }
  }

  def mapFutureList[B](f: Seq[A] => Fu[Seq[B]]): AdapterLike[B] = new AdapterLike[B] {

    def nbResults = AdapterLike.this.nbResults

    def slice(offset: Int, length: Int) =
      AdapterLike.this.slice(offset, length) flatMap f
  }
}
