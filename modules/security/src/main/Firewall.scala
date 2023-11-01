package lila.security

import play.api.mvc.RequestHeader

import lila.common.IpAddress
import lila.db.dsl.{ *, given }

final class Firewall(
    coll: Coll,
    scheduler: Scheduler
)(using Executor):

  private var current: Set[String] = Set.empty

  scheduler.scheduleOnce(49.seconds):
    loadFromDb

  def blocksIp(ip: IpAddress): Boolean = current contains ip.value

  def blocks(req: RequestHeader): Boolean =
    val v = blocksIp:
      lila.common.HTTPRequest ipAddress req
    if v then lila.mon.security.firewall.block.increment()
    v

  def accepts(req: RequestHeader): Boolean = !blocks(req)

  def blockIps(ips: Seq[IpAddress]): Funit =
    ips.traverse_ { ip =>
      coll.update
        .one(
          $id(ip),
          $doc("_id" -> ip, "date" -> nowInstant),
          upsert = true
        )
        .void
    } >> loadFromDb

  def unblockIps(ips: Iterable[IpAddress]): Funit =
    coll.delete.one($inIds(ips)).void andDo loadFromDb

  private def loadFromDb: Funit =
    coll.distinctEasy[String, Set]("_id", $empty).map { ips =>
      current = ips
      lila.mon.security.firewall.ip.update(ips.size)
    }
