package lila.security

import reactivemongo.api.bson.*

import lila.core.security.FingerHash
import lila.db.dsl.{ *, given }

final class PrintBan(coll: Coll)(using Executor):

  private var current: Set[String] = Set.empty

  def blocks(hash: FingerHash): Boolean = current contains hash.value

  def toggle(hash: FingerHash, block: Boolean): Funit =
    current = if block then current + hash.value else current - hash.value
    if block then
      coll.update
        .one(
          $id(hash.value),
          $doc("_id" -> hash.value, "date" -> nowInstant),
          upsert = true
        )
        .void
    else coll.delete.one($id(hash.value)).void

  coll.secondaryPreferred.distinctEasy[String, Set]("_id", $empty).map { hashes =>
    current = hashes
    lila.mon.security.firewall.prints.update(hashes.size)
  }
