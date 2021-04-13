package lila.security

import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.bson.BSONNull
import reactivemongo.api.bson.{ BSONHandler, Macros }
import reactivemongo.api.CursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.blocking
import scala.concurrent.duration._

import lila.common.{ ApiVersion, HTTPRequest, IpAddress }
import lila.db.dsl._
import lila.user.User

final class Store(val coll: Coll, cacheApi: lila.memo.CacheApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import Store._

  private val authCache = cacheApi[String, Option[AuthInfo]](32768, "security.authCache") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(65536)
      .buildAsyncFuture[String, Option[AuthInfo]] { id =>
        coll
          .find($doc("_id" -> id, "up" -> true), authInfoProjection.some)
          .one[Bdoc]
          .map {
            _.flatMap { doc =>
              if (doc.getAsOpt[DateTime]("date").fold(true)(_ isBefore DateTime.now.minusHours(12)))
                coll.updateFieldUnchecked($id(id), "date", DateTime.now)
              doc.getAsOpt[User.ID]("user") map { AuthInfo(_, doc.contains("fp")) }
            }
          }
      }
  }

  def authInfo(sessionId: String) = authCache get sessionId

  private val authInfoProjection = $doc("user" -> true, "fp" -> true, "date" -> true, "_id" -> false)
  private def uncache(sessionId: String) =
    blocking { blockingUncache(sessionId) }
  private def uncacheAllOf(userId: User.ID): Funit =
    coll.distinctEasy[String, Seq]("_id", $doc("user" -> userId)) map { ids =>
      blocking {
        ids foreach blockingUncache
      }
    }
  // blocks loading values! https://github.com/ben-manes/caffeine/issues/148
  private def blockingUncache(sessionId: String) =
    authCache.underlying.synchronous.invalidate(sessionId)

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
          "ip"   -> HTTPRequest.ipAddress(req),
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
    coll
      .find($doc("user" -> userId, "up" -> true))
      .sort($doc("date" -> -1))
      .cursor[UserSession]()
      .gather[List](nb)

  def allSessions(userId: User.ID): AkkaStreamCursor[UserSession] =
    coll
      .find($doc("user" -> userId))
      .sort($doc("date" -> -1))
      .cursor[UserSession](ReadPreference.secondaryPreferred)

  def setFingerPrint(id: String, fp: FingerPrint): Fu[FingerHash] =
    FingerHash(fp) match {
      case None => fufail(s"Can't hash $id's fingerprint $fp")
      case Some(hash) =>
        coll.updateField($id(id), "fp", hash) >>- {
          authInfo(id) foreach {
            _ foreach { i =>
              authCache.put(id, fuccess(i.copy(hasFp = true).some))
            }
          }
        } inject hash
    }

  def chronoInfoByUser(user: User): Fu[List[Info]] =
    coll
      .find(
        $doc(
          "user" -> user.id,
          "date" $gt (user.createdAt atLeast DateTime.now.minusYears(1))
        ),
        $doc("_id" -> false, "ip" -> true, "ua" -> true, "fp" -> true, "date" -> true).some
      )
      .sort($sort desc "date")
      .cursor[Info]()(InfoReader, implicitly[CursorProducer[Info]])
      .list(1000)

  // remains of never-confirmed accounts that got cleaned up
  private[security] def deletePreviousSessions(user: User) =
    coll.delete.one($doc("user" -> user.id, "date" $lt user.createdAt)).void

  private case class DedupInfo(_id: String, ip: String, ua: String) {
    def compositeKey = s"$ip $ua"
  }
  implicit private val DedupInfoReader = Macros.reader[DedupInfo]

  def dedup(userId: User.ID, keepSessionId: String): Funit =
    coll
      .find(
        $doc(
          "user" -> userId,
          "up"   -> true
        )
      )
      .sort($doc("date" -> -1))
      .cursor[DedupInfo]()
      .list()
      .flatMap { sessions =>
        val olds = sessions
          .groupBy(_.compositeKey)
          .view
          .values
          .flatMap(_ drop 1)
          .filter(_._id != keepSessionId)
          .map(_._id)
        coll.delete.one($inIds(olds)).void
      } >> uncacheAllOf(userId)

  implicit private val IpAndFpReader = Macros.reader[IpAndFp]

  def shareAnIpOrFp(u1: User.ID, u2: User.ID): Fu[Boolean] =
    coll.aggregateExists(ReadPreference.secondaryPreferred) { framework =>
      import framework._
      Match($doc("user" $in List(u1, u2))) -> List(
        Limit(500),
        Project(
          $doc(
            "_id"  -> false,
            "user" -> true,
            "x"    -> $arr("$ip", "$fp")
          )
        ),
        UnwindField("x"),
        GroupField("x")("users" -> AddFieldToSet("user")),
        Match(
          $doc(
            "_id" $ne BSONNull,
            "users.1" $exists true
          )
        ),
        Limit(1)
      )
    }

  def ips(user: User): Fu[Set[IpAddress]] =
    coll.distinctEasy[IpAddress, Set]("ip", $doc("user" -> user.id))

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

  case class Info(ip: IpAddress, ua: String, fp: Option[FingerHash], date: DateTime) {
    def datedIp = Dated(ip, date)
    def datedFp = fp.map { Dated(_, date) }
    def datedUa = Dated(ua, date)
  }

  implicit val fingerHashBSONHandler: BSONHandler[FingerHash] = stringIsoHandler[FingerHash]
  implicit val InfoReader                                     = Macros.reader[Info]
}
