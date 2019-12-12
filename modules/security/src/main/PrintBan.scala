package lila.security

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

final class PrintBan(coll: Coll) {

  private var current: Set[String] = Set.empty

  def blocks(hash: FingerHash): Boolean = current contains hash.value

  def toggle(hash: FingerHash, block: Boolean): Funit = {
    if (block) coll.update(
      $id(hash.value),
      $doc("_id" -> hash.value, "date" -> DateTime.now),
      upsert = true
    ).void
    else coll.remove($id(hash.value))
  } >> loadFromDb

  private def loadFromDb: Funit =
    coll.distinct[String, Set]("_id", none).map { hashes =>
      current = hashes
      lila.mon.security.firewall.prints(hashes.size)
    }
}
