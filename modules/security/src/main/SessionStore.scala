package lila.security

import play.api.mvc.RequestHeader
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.bson.{ BSONDocumentHandler, BSONDocumentReader, BSONNull, Macros }

import scala.concurrent.blocking

import lila.common.HTTPRequest
import lila.core.id.SessionId
import lila.core.net.{ ApiVersion, IpAddress, UserAgent }
import lila.core.misc.oauth.AccessTokenId
import lila.core.security.FingerHash
import lila.core.socket.Sri
import lila.db.dsl.{ *, given }

final class SessionStore(val coll: Coll, cacheApi: lila.memo.CacheApi)(using Executor):

  import SessionStore.*
  import FingerHash.given

  private val authCache = cacheApi[SessionId, Option[AuthInfo]](65_536, "security.session.info"):
    _.expireAfterAccess(5.minutes).buildAsyncFuture[SessionId, Option[AuthInfo]]: id =>
      coll
        .find($doc("_id" -> id, "up" -> true))
        .one[Bdoc]
        .map:
          _.flatMap: doc =>
            if doc.getAsOpt[Instant]("date").forall(_.isBefore(nowInstant.minusHours(12))) then
              coll.updateFieldUnchecked($id(id), "date", nowInstant)
            doc.getAsOpt[UserId]("user").map { AuthInfo(_, doc.contains("fp")) }

  def authInfo(sessionId: SessionId) = authCache.get(sessionId)

  private def uncache(sessionId: SessionId): Unit =
    blocking { blockingUncache(sessionId) }
  private def uncacheAllOf(userId: UserId): Funit =
    coll.distinctEasy[SessionId, Seq]("_id", $doc("user" -> userId)).map { ids =>
      blocking:
        ids.foreach(blockingUncache)
    }
  // blocks loading values! https://github.com/ben-manes/caffeine/issues/148
  private def blockingUncache(sessionId: SessionId) =
    authCache.underlying.synchronous.invalidate(sessionId)

  private[security] def save(
      isSignup: Boolean,
      userId: UserId,
      req: RequestHeader,
      apiVersion: Option[ApiVersion],
      fp: Option[FingerPrint],
      proxy: lila.core.security.IsProxy,
      pwned: IsPwned
  ): Fu[SessionId] =
    val sessionId = SessionId(scalalib.SecureRandom.nextString(22))
    val baseDoc = $doc(
      "user" -> userId,
      "ip" -> HTTPRequest.ipAddress(req),
      "ua" -> HTTPRequest.userAgent(req).some.filter(_ != UserAgent.zero)
    )
    val newDoc = baseDoc ++ $doc(
      "_id" -> sessionId,
      "date" -> nowInstant,
      "api" -> apiVersion, // lichobile
      "fp" -> fp.flatMap(lila.security.FingerHash.from),
      "proxy" -> proxy.yes.option(proxy),
      "pwned" -> pwned.yes.option(true)
    )
    val updateDb =
      if isSignup then
        val doc = newDoc ++ $doc("signup" -> true, "up" -> false)
        coll.insert.one(doc).void
      else
        val prevSelector = baseDoc ++ $doc("up" -> false, "signup".$ne(true))
        for
          _ <- coll.delete.one(prevSelector)
          doc = newDoc ++ $doc("up" -> true)
          _ <- coll.insert.one(doc)
        yield ()
    for _ <- updateDb yield sessionId

  private[security] def upsertOAuth(
      userId: UserId,
      tokenId: AccessTokenId,
      mobile: Option[lila.core.net.LichessMobileUa],
      req: RequestHeader
  ): Funit =
    val id = s"TOK-${tokenId.value.take(20)}"
    val ua = mobile
      .map(Mobile.LichessMobileUaTrim.write)
      .getOrElse(HTTPRequest.userAgent(req).value)
    coll.update
      .one(
        $id(id),
        $doc(
          "_id" -> id,
          "user" -> userId,
          "ip" -> HTTPRequest.ipAddress(req),
          "ua" -> ua,
          "date" -> nowInstant,
          "up" -> true,
          "fp" -> mobile.map(_.sri.value)
        ),
        upsert = true
      )
      .void

  def delete(sessionId: SessionId): Funit =
    for _ <- coll.update.one($id(sessionId), $set("up" -> false))
    yield uncache(sessionId)

  def closeUserAndSessionId(userId: UserId, sessionId: SessionId): Funit =
    for _ <- coll.update.one($doc("user" -> userId, "_id" -> sessionId, "up" -> true), $set("up" -> false))
    yield uncache(sessionId)

  def closeUserExceptSessionId(userId: UserId, sessionId: SessionId): Funit =
    for _ <- coll.update.one(
        $doc("user" -> userId, "_id" -> $ne(sessionId), "up" -> true),
        $set("up" -> false),
        multi = true
      )
    yield uncacheAllOf(userId)

  def closeAllSessionsOf(userId: UserId): Funit =
    for _ <- coll.update.one(
        $doc("user" -> userId, "up" -> true),
        $set("up" -> false),
        multi = true
      )
    yield uncacheAllOf(userId)

  def deleteAllSessionsOf(userId: UserId): Funit =
    for _ <- coll.delete.one($doc("user" -> userId))
    yield uncacheAllOf(userId)

  private given BSONDocumentHandler[UserSession] = Macros.handler[UserSession]
  def openSessions(userId: UserId, nb: Int): Fu[List[UserSession]] =
    coll
      .find($doc("user" -> userId, "up" -> true))
      .sort($doc("date" -> -1))
      .cursor[UserSession](ReadPref.sec)
      .list(nb)

  def allSessions(userId: UserId): AkkaStreamCursor[UserSession] =
    coll
      .find($doc("user" -> userId))
      .sort($doc("date" -> -1))
      .cursor[UserSession](ReadPref.sec)

  def setFingerPrint(id: SessionId, fp: FingerPrint): Fu[FingerHash] =
    lila.security.FingerHash.from(fp) match
      case None => fufail(s"Can't hash $id's fingerprint $fp")
      case Some(hash) =>
        for
          _ <- coll.updateField($id(id), "fp", hash)
          _ = authInfo(id).foreach:
            _.foreach: i =>
              authCache.put(id, fuccess(i.copy(hasFp = true).some))
        yield hash

  def chronoInfoByUser(user: User): Fu[List[Info]] =
    coll
      .find(
        $doc(
          "user" -> user.id,
          "date".$gt(nowInstant.minusYears(1))
        )
      )
      .sort($sort.desc("date"))
      .cursor[Info](ReadPref.sec)
      .list(1000)

  // remains of never-confirmed accounts that got cleaned up
  private[security] def deletePreviousSessions(user: User) =
    coll.delete.one($doc("user" -> user.id, "date".$lt(user.createdAt))).void

  private case class DedupInfo(_id: SessionId, ip: String, ua: String):
    def compositeKey = s"$ip $ua"
  private given BSONDocumentReader[DedupInfo] = Macros.reader

  def dedup(userId: UserId, keepSessionId: SessionId): Funit = for
    sessions <- coll
      .find($doc("user" -> userId, "up" -> true))
      .sort($doc("date" -> -1))
      .cursor[DedupInfo]()
      .list(1000)
    olds = sessions
      .groupBy(_.compositeKey)
      .view
      .values
      .flatMap(_.drop(1))
      .filter(_._id != keepSessionId)
      .map(_._id)
    _ <- coll.delete.one($inIds(olds)).void
    _ <- uncacheAllOf(userId)
  yield ()

  def shareAnIpOrFp(users: PairOf[UserId]): Fu[Boolean] =
    coll.aggregateExists(_.sec): framework =>
      import framework.*
      Match($doc("user".$in(users.asList))) -> List(
        Limit(500),
        Project(
          $doc(
            "_id" -> false,
            "user" -> true,
            "x" -> $arr("$ip", "$fp")
          )
        ),
        UnwindField("x"),
        GroupField("x")("users" -> AddFieldToSet("user")),
        Match(
          $doc(
            "_id".$ne(BSONNull),
            "users.1".$exists(true)
          )
        ),
        Limit(1)
      )

  private[security] def recentByIpExists(ip: IpAddress, since: FiniteDuration): Fu[Boolean] =
    coll.secondary.exists:
      $doc("ip" -> ip, "date" -> $gt(nowInstant.minusMinutes(since.toMinutes.toInt)))

  private[security] def recentByPrintExists(fp: FingerPrint): Fu[Boolean] =
    lila.security.FingerHash.from(fp).so { hash =>
      coll.secondary.exists:
        $doc("fp" -> hash, "date" -> $gt(nowInstant.minusDays(7)))
    }

object SessionStore:

  case class Info(ip: IpAddress, ua: UserAgent, fp: Option[FingerHash], date: Instant):
    def datedIp = Dated(ip, date)
    def datedFp = fp.map { Dated(_, date) }
    def datedUa = Dated(ua, date)

  given BSONDocumentReader[Info] = Macros.reader[Info]
