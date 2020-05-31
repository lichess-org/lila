package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.{ BSONHandler, Macros }
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.util.Random

import lila.common.{ ApiVersion, HTTPRequest, IpAddress }
import lila.db.dsl._
import lila.user.User

final class Store(val coll: Coll, cacheApi: lila.memo.CacheApi, localIp: IpAddress)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import Store._

  private val authCache = cacheApi[String, Option[AuthInfo]](32768, "security.authCache") {
    _.expireAfterWrite(1 minute)
      .maximumSize(65536)
      .buildAsyncFuture[String, Option[AuthInfo]] { id =>
        coll.find($doc("_id" -> id, "up" -> true), authInfoProjection.some).one[AuthInfo]
      }
  }

  def authInfo(sessionId: String) = authCache get sessionId

  def setNow(sessionId: String, i: AuthInfo): Unit = {
    val info = i.copy(date = DateTime.now)
    coll.updateFieldUnchecked($id(sessionId), "date", info.date)
    authCache.put(sessionId, fuccess(info.some))
  }

  implicit private val AuthInfoReader    = Macros.reader[AuthInfo]
  private val authInfoProjection         = $doc("user" -> true, "fp" -> true, "date" -> true, "_id" -> false)
  private def uncache(sessionId: String) = authCache.put(sessionId, fuccess(none))
  private def uncacheAllOf(userId: User.ID): Funit =
    coll.distinctEasy[String, Seq]("_id", $doc("user" -> userId, "up" -> true)) map {
      _ foreach uncache
    }

  def save(
      sessionId: String,
      userId: User.ID,
      req: RequestHeader,
      apiVersion: Option[ApiVersion],
      up: Boolean,
      fp: Option[FingerPrint]
  ): Funit =
    coll.insert
      .one(
        $doc(
          "_id"  -> sessionId,
          "user" -> userId,
          "ip" -> (HTTPRequest.lastRemoteAddress(req) match {
            // randomize stresser IPs to relieve mod tools
            case ip if ip == localIp => IpAddress(s"127.0.${Random nextInt 256}.${Random nextInt 256}")
            case ip                  => ip
          }),
          "ua"   -> HTTPRequest.userAgent(req).|("?"),
          "date" -> DateTime.now,
          "up"   -> up,
          "api"  -> apiVersion.map(_.value),
          "fp"   -> fp.flatMap(FingerHash.apply).flatMap(fingerHashBSONHandler.writeOpt)
        )
      )
      .void

  def delete(sessionId: String): Funit =
    coll.update
      .one(
        $id(sessionId),
        $set("up" -> false)
      )
      .void >>- uncache(sessionId)

  def closeUserAndSessionId(userId: User.ID, sessionId: String): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "_id" -> sessionId, "up" -> true),
        $set("up"   -> false)
      )
      .void >>- uncache(sessionId)

  def closeUserExceptSessionId(userId: User.ID, sessionId: String): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "_id" -> $ne(sessionId), "up" -> true),
        $set("up"   -> false),
        multi = true
      )
      .void >> uncacheAllOf(userId)

  def closeAllSessionsOf(userId: User.ID): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "up" -> true),
        $set("up"   -> false),
        multi = true
      )
      .void >> uncacheAllOf(userId)

  implicit private val UserSessionBSONHandler = Macros.handler[UserSession]
  def openSessions(userId: User.ID, nb: Int): Fu[List[UserSession]] =
    coll.ext
      .find(
        $doc("user" -> userId, "up" -> true)
      )
      .sort($doc("date" -> -1))
      .cursor[UserSession]()
      .gather[List](nb)

  def setFingerPrint(id: String, fp: FingerPrint): Fu[FingerHash] =
    FingerHash(fp) match {
      case None       => fufail(s"Can't hash $id's fingerprint $fp")
      case Some(hash) => coll.updateField($id(id), "fp", hash) >>- uncache(id) inject hash
    }

  def chronoInfoByUser(user: User): Fu[List[Info]] =
    coll.ext
      .find(
        $doc(
          "user" -> user.id,
          "date" $gt (user.createdAt atLeast DateTime.now.minusYears(1))
        ),
        $doc("_id" -> false, "ip" -> true, "ua" -> true, "fp" -> true, "date" -> true)
      )
      .sort($sort desc "date")
      .list[Info](1000)(InfoReader)

  // remains of never-confirmed accounts that got cleaned up
  private[security] def deletePreviousSessions(user: User) =
    coll.delete.one($doc("user" -> user.id, "date" $lt user.createdAt)).void

  private case class DedupInfo(_id: String, ip: String, ua: String) {
    def compositeKey = s"$ip $ua"
  }
  implicit private val DedupInfoReader = Macros.reader[DedupInfo]

  def dedup(userId: User.ID, keepSessionId: String): Funit =
    coll.ext
      .find(
        $doc(
          "user" -> userId,
          "up"   -> true
        )
      )
      .sort($doc("date" -> -1))
      .list[DedupInfo]()
      .flatMap { sessions =>
        val olds = sessions
          .groupBy(_.compositeKey)
          .view
          .values
          .map(_ drop 1)
          .flatten
          .filter(_._id != keepSessionId)
          .map(_._id)
        coll.delete.one($inIds(olds)).void
      } >> uncacheAllOf(userId)

  implicit private val IpAndFpReader = Macros.reader[IpAndFp]

  def ipsAndFps(userIds: List[User.ID], max: Int = 100): Fu[List[IpAndFp]] =
    coll.ext.find($doc("user" $in userIds)).list[IpAndFp](max, ReadPreference.secondaryPreferred)

  private[security] def recentByIpExists(ip: IpAddress): Fu[Boolean] =
    coll.secondaryPreferred.exists(
      $doc("ip" -> ip, "date" -> $gt(DateTime.now minusDays 7))
    )

  private[security] def recentByPrintExists(fp: FingerPrint): Fu[Boolean] =
    FingerHash(fp) ?? { hash =>
      coll.secondaryPreferred.exists(
        $doc("fp" -> hash, "date" -> $gt(DateTime.now minusDays 7))
      )
    }
}

object Store {

  case class Dated[V](value: V, date: DateTime) extends Ordered[Dated[V]] {
    def compare(other: Dated[V]) = other.date compareTo date
  }

  case class Info(ip: IpAddress, ua: String, fp: Option[FingerHash], date: DateTime) {
    def datedIp = Dated(ip, date)
    def datedFp = fp.map { Dated(_, date) }
    def datedUa = Dated(ua, date)
  }

  implicit val fingerHashBSONHandler: BSONHandler[FingerHash] = stringIsoHandler[FingerHash]
  implicit val InfoReader                                     = Macros.reader[Info]
}
