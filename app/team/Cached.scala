package lila
package team

import scala.collection.mutable

final class Cached(teamRepo: TeamRepo) {

  def name(id: String): Option[String] =
    nameCache.getOrElseUpdate(
      id.toLowerCase,
      (teamRepo name id).unsafePerformIO 
    )

  // id => name
  private val nameCache = mutable.Map[String, Option[String]]()
}
