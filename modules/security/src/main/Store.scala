package lila.security

import play.api.mvc.RequestHeader
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.bson.{ BSONDocumentHandler, BSONDocumentReader, BSONNull, Macros }
import reactivemongo.api.ReadPreference
import scala.concurrent.blocking

import lila.common.{ ApiVersion, HTTPRequest, IpAddress }
import lila.db.dsl.{ *, given }
import lila.user.User

final class Store(val coll: Coll, cacheApi: lila.memo.CacheApi)(using
    ec: Executor
):

  import Store.*
  import FingerHash.given

  private val authCache = cacheApi[String, Option[AuthInfo]](65536, "security.authCache") {
    _.expireAfterAccess(5 minutes)
      .buildAsyncFuture[String, Option[AuthInfo]] { id =>
        coll
          .find($doc("_id" -> id, "up" -> true), authInfoProjection.some)
          .one[Bdoc]
          .map {
            _.flatMap { doc =>
              if (doc.getAsOpt[Instant]("date").fold(true)(_ isBefore nowInstant.minusHours(12)))
                coll.updateFieldUnchecked($id(id), "date", nowInstant)
              doc.getAsOpt[UserId]("user") map { AuthInfo(_, doc.contains("fp")) }
            }
          }
      }
  }

  def authInfo(sessionId: String) = authCache get sessionId

  private val authInfoProjection = $doc("user" -> true, "fp" -> true, "date" -> true, "_id" -> false)
  private def uncache(sessionId: String) =
    blocking { blockingUncache(sessionId) }
  private def uncacheAllOf(userId: UserId): Funit =
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
      userId: UserId,
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
          "ua"   -> HTTPRequest.userAgent(req).fold("?")(_.value),
          "date" -> nowInstant,
          "up"   -> up,
          "api"  -> apiVersion,
          "fp"   -> fp.flatMap(FingerHash.from)
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

  def closeUserAndSessionId(userId: UserId, sessionId: String): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "_id" -> sessionId, "up" -> true),
        $set("up"   -> false)
      )
      .void >>- uncache(sessionId)

  def closeUserExceptSessionId(userId: UserId, sessionId: String): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "_id" -> $ne(sessionId), "up" -> true),
        $set("up"   -> false),
        multi = true
      )
      .void >> uncacheAllOf(userId)

  def closeAllSessionsOf(userId: UserId): Funit =
    coll.update
      .one(
        $doc("user" -> userId, "up" -> true),
        $set("up"   -> false),
        multi = true
      )
      .void >> uncacheAllOf(userId)

  private given BSONDocumentHandler[UserSession] = Macros.handler[UserSession]
  def openSessions(userId: UserId, nb: Int): Fu[List[UserSession]] =
    coll
      .find($doc("user" -> userId, "up" -> true))
      .sort($doc("date" -> -1))
      .cursor[UserSession]()
      .list(nb)

  def allSessions(userId: UserId): AkkaStreamCursor[UserSession] =
    coll
      .find($doc("user" -> userId))
      .sort($doc("date" -> -1))
      .cursor[UserSession](temporarilyPrimary)

  def setFingerPrint(id: String, fp: FingerPrint): Fu[FingerHash] =
    FingerHash.from(fp) match
      case None => fufail(s"Can't hash $id's fingerprint $fp")
      case Some(hash) =>
        coll.updateField($id(id), "fp", hash) >>- {
          authInfo(id) foreach {
            _ foreach { i =>
              authCache.put(id, fuccess(i.copy(hasFp = true).some))
            }
          }
        } inject hash

  def chronoInfoByUser(user: User): Fu[List[Info]] =
    coll
      .find(
        $doc(
          "user" -> user.id,
          "date" $gt (user.createdAt atLeast nowInstant.minusYears(1))
        ),
        $doc("_id" -> false, "ip" -> true, "ua" -> true, "fp" -> true, "date" -> true).some
      )
      .sort($sort desc "date")
      .cursor[Info]()
      .list(1000)

  // remains of never-confirmed accounts that got cleaned up
  private[security] def deletePreviousSessions(user: User) =
    coll.delete.one($doc("user" -> user.id, "date" $lt user.createdAt)).void

  private case class DedupInfo(_id: String, ip: String, ua: String):
    def compositeKey = s"$ip $ua"
  private given BSONDocumentReader[DedupInfo] = Macros.reader

  def dedup(userId: UserId, keepSessionId: String): Funit =
    coll
      .find(
        $doc(
          "user" -> userId,
          "up"   -> true
        )
      )
      .sort($doc("date" -> -1))
      .cursor[DedupInfo]()
      .list(1000)
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

  def shareAnIpOrFp(u1: UserId, u2: UserId): Fu[Boolean] =
    coll.aggregateExists(ReadPreference.secondaryPreferred) { framework =>
      import framework.*
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

  private[security] def recentByIpExists(ip: IpAddress, since: FiniteDuration): Fu[Boolean] =
    coll.secondaryPreferred.exists(
      $doc("ip" -> ip, "date" -> $gt(nowInstant minusMinutes since.toMinutes.toInt))
    )

  private[security] def recentByPrintExists(fp: FingerPrint): Fu[Boolean] =
    FingerHash.from(fp).so { hash =>
      coll.secondaryPreferred.exists:
        $doc("fp" -> hash, "date" -> $gt(nowInstant minusDays 7))
    }

object Store:

  case class Info(ip: IpAddress, ua: UserAgent, fp: Option[FingerHash], date: Instant):
    def datedIp = Dated(ip, date)
    def datedFp = fp.map { Dated(_, date) }
    def datedUa = Dated(ua, date)

  given BSONDocumentReader[Info] = Macros.reader[Info]
