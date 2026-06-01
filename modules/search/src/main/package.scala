package lila.search

export lila.common.extensions.*
export lila.core.lilaism.Lilaism.{ *, given }

private lazy val logger = lila.log("lilaSearch")

trait SearchApi[A, Q]:
  def search(query: Q, offset: Long, length: Long): Fu[List[A]]
  def count(query: Q): Fu[Long]
