package lila.security

import play.api.mvc.RequestHeader
import reactivemongo.api.ReadPreference

import lila.common.IpAddress
import lila.db.dsl.{ *, given }

final class Firewall(
    coll: Coll,
    scheduler: Scheduler
)(using Executor):

  private var current: Set[String] = Set.empty

  scheduler.scheduleOnce(10 minutes)(loadFromDb.unit)

  def blocksIp(ip: IpAddress): Boolean = current contains ip.value

  def blocks(req: RequestHeader): Boolean =
    val v = blocksIp {
      lila.common.HTTPRequest ipAddress req
    }
    if (v) lila.mon.security.firewall.block.increment()
    v

  def accepts(req: RequestHeader): Boolean = !blocks(req)

  def blockIps(ips: Iterable[IpAddress]): Funit =
    ips.map { ip =>
      coll.update
        .one(
          $id(ip),
          $doc("_id" -> ip, "date" -> nowInstant),
          upsert = true
        )
        .void
    }.parallel >> loadFromDb

  def unblockIps(ips: Iterable[IpAddress]): Funit =
    coll.delete.one($inIds(ips)).void >>- loadFromDb.unit

  private def loadFromDb: Funit =
    coll.distinctEasy[String, Set]("_id", $empty, ReadPreference.primary).map { ips =>
      current = ips
      lila.mon.security.firewall.ip.update(ips.size).unit
    }
