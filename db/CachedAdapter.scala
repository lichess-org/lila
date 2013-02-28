package lila.db

import com.github.ornicar.paginator._
import com.novus.salat.dao.DAO
import com.novus.salat._
import com.mongodb.casbah.Imports.DBObject

final class CachedAdapter[A <: CaseClass, B <: Any](
    dao: DAO[A, B],
    query: DBObject,
    sort: DBObject,
    val nbResults: Int) extends Adapter[A] {

  private val salatAdapter = SalatAdapter(
    dao = dao,
    query = query,
    sort = sort)

  def slice(offset: Int, length: Int) = salatAdapter.slice(offset, length)
}
