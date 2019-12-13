package lila.security

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._

final class PrintBan(coll: Coll) {

  private var current: Set[String] = Set.empty

  def blocks(hash: FingerHash): Boolean = current contains hash.value

  def toggle(hash: FingerHash, block: Boolean): Funit = {
    if (block)
      coll.update
        .one(
          $id(hash.value),
          $doc("_id" -> hash.value, "date" -> DateTime.now),
          upsert = true
        )
        .void
    else coll.delete.one($id(hash.value))
  } >> loadFromDb

  private def loadFromDb: Funit =
    coll.distinctEasy[String, Set]("_id", $empty).map { hashes =>
      current = hashes
      lila.mon.security.firewall.prints.update(hashes.size)
    }
}
