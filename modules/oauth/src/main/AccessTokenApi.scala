package lila.oauth

import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.common.Json.given
import lila.core.misc.oauth.TokenRevoke
import lila.core.net.Bearer
import lila.db.dsl.{ *, given }

final class AccessTokenApi(
    coll: Coll,
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi
)(using Executor):

  import OAuthScope.given
  import AccessToken.{ BSONFields as F, given }

  private def createAndRotate(token: AccessToken): Fu[AccessToken] = for
    oldIds <- coll
      .find($doc(F.userId -> token.userId, F.clientOrigin -> token.clientOrigin), $doc(F.id -> true).some)
      .sort($doc(F.usedAt -> -1, F.createdAt -> -1))
      .skip(30)
      .cursor[Bdoc]()
      .listAll()
      .dmap:
        _.flatMap { _.getAsOpt[AccessToken.Id](F.id) }
    _ <- oldIds.nonEmpty.so:
      coll.delete.one($doc(F.id.$in(oldIds))).void
    _ <- coll.insert.one(token)
  yield token

  def create(setup: OAuthTokenForm.Data, isStudent: Boolean)(using me: MyId): Fu[AccessToken] =
    (fuccess(isStudent) >>| userApi.isManaged(me)).flatMap { noBot =>
      val plain = Bearer.randomPersonal()
      createAndRotate:
        AccessToken(
          id = AccessToken.Id.from(plain),
          plain = plain,
          userId = me,
          description = setup.description.some,
          createdAt = nowInstant.some,
          scopes = TokenScopes:
            setup.scopes
              .flatMap(OAuthScope.byKey.get)
              .filterNot(_ == OAuthScope.Bot.Play && noBot)
              .filterNot(_ == OAuthScope.Web.Mobile)
              .toList
          ,
          clientOrigin = None,
          expires = None
        )
    }

  def create(granted: AccessTokenRequest.Granted): Fu[AccessToken] =
    val plain = Bearer.random()
    createAndRotate:
      AccessToken(
        id = AccessToken.Id.from(plain),
        plain = plain,
        userId = granted.userId,
        description = None,
        createdAt = nowInstant.some,
        scopes = granted.scopes,
        clientOrigin = granted.redirectUri.clientOrigin.some,
        expires = nowInstant.plusMonths(12).some
      )

  def adminChallengeTokens(
      setup: OAuthTokenForm.AdminChallengeTokensData,
      admin: User
  ): Fu[Map[UserId, AccessToken]] =
    userApi
      .enabledByIds(setup.usernames)
      .flatMap: users =>
        val scope = OAuthScope.Challenge.Write
        users
          .sequentially: user =>
            coll
              .one[AccessToken]:
                $doc(
                  F.userId       -> user.id,
                  F.clientOrigin -> setup.description,
                  F.scopes       -> scope.key
                )
              .getOrElse:
                val plain = Bearer.randomPersonal()
                createAndRotate:
                  AccessToken(
                    id = AccessToken.Id.from(plain),
                    plain = plain,
                    userId = user.id,
                    description = s"Challenge admin: ${admin.username}".some,
                    createdAt = nowInstant.some,
                    scopes = TokenScopes(List(scope)),
                    clientOrigin = setup.description.some,
                    expires = Some(nowInstant.plusMonths(6))
                  )
              .map(user.id -> _)
          .map(_.toMap)

  def listPersonal(using me: MyId): Fu[List[AccessToken]] =
    coll
      .find:
        $doc(
          F.userId       -> me,
          F.clientOrigin -> $exists(false)
        )
      .sort($sort.desc(F.createdAt)) // c.f. isBrandNew
      .cursor[AccessToken]()
      .list(100)

  def usedBoardApi(user: UserId): Fu[List[AccessToken]] =
    coll
      .find:
        $doc(
          F.scopes -> OAuthScope.Board.Play.key,
          F.usedAt.$exists(true),
          F.userId -> user
        )
      .sort($sort.desc(F.createdAt))
      .cursor[AccessToken]()
      .list(30)

  def countPersonal(using me: MyId): Fu[Int] =
    coll.countSel:
      $doc(
        F.userId       -> me,
        F.clientOrigin -> $exists(false)
      )

  def findCompatiblePersonal(scopes: OAuthScopes)(using me: MyId): Fu[Option[AccessToken]] =
    coll.one[AccessToken]:
      $doc(
        F.userId       -> me,
        F.clientOrigin -> $exists(false),
        F.scopes.$all(scopes.value)
      )

  def listClients(limit: Int)(using me: MyId): Fu[List[AccessTokenApi.Client]] =
    coll
      .aggregateList(limit): framework =>
        import framework.*
        Match(
          $doc(
            F.userId       -> me,
            F.clientOrigin -> $exists(true)
          )
        ) -> List(
          Unwind(path = F.scopes, includeArrayIndex = None, preserveNullAndEmptyArrays = Some(true)),
          GroupField(F.clientOrigin)(
            F.usedAt -> MaxField(F.usedAt),
            F.scopes -> AddFieldToSet(F.scopes)
          ),
          Sort(Descending(F.usedAt)),
          Limit(limit)
        )
      .map: docs =>
        for
          doc    <- docs
          origin <- doc.getAsOpt[String]("_id")
          usedAt = doc.getAsOpt[Instant](F.usedAt)
          scopes <- doc.getAsOpt[List[OAuthScope]](F.scopes)(using collectionReader)
        yield AccessTokenApi.Client(origin, usedAt, scopes)

  def revokeById(id: AccessToken.Id)(using me: MyId): Funit =
    for _ <- coll.delete.one($doc(F.id -> id, F.userId -> me))
    yield onRevoke(id)

  def revokeByClientOrigin(clientOrigin: String)(using me: MyId): Funit =
    coll
      .find(
        $doc(
          F.userId       -> me,
          F.clientOrigin -> clientOrigin
        ),
        $doc(F.id -> 1).some
      )
      .sort($sort.desc(F.usedAt))
      .cursor[Bdoc]()
      .list(100)
      .flatMap: invalidate =>
        coll.delete
          .one:
            $doc(
              F.userId       -> me,
              F.clientOrigin -> clientOrigin
            )
          .map(_ => invalidate.flatMap(_.getAsOpt[AccessToken.Id](F.id)).foreach(onRevoke))

  def revoke(bearer: Bearer) =
    val id = AccessToken.Id.from(bearer)
    for _ <- coll.delete.one($id(id)) yield onRevoke(id)

  private[oauth] def get(bearer: Bearer) = accessTokenCache.get(AccessToken.Id.from(bearer))

  def test(bearers: List[Bearer]): Fu[Map[Bearer, Option[AccessToken]]] =
    coll
      .optionsByOrderedIds[AccessToken, AccessToken.Id](
        bearers.map(AccessToken.Id.from),
        readPref = _.sec
      )(_.id)
      .flatMap: tokens =>
        userApi.filterDisabled(tokens.flatten.map(_.userId)).map { closedUserIds =>
          val openTokens = tokens.map(_.filter(token => !closedUserIds(token.userId)))
          bearers.zip(openTokens).toMap
        }

  def secretScanning(scans: List[AccessTokenApi.GithubSecretScan]): Fu[List[(AccessToken, String)]] = for
    found <- test(scans.map(_.token))
    res <- scans.sequentially: scan =>
      val compromised = found.get(scan.token).flatten
      lila.mon.security.secretScanning(scan.`type`, scan.source, compromised.isDefined).increment()
      compromised match
        case Some(token) =>
          logger.branch("github").info(s"revoking token ${token.plain} for user ${token.userId}")
          revoke(token.plain).inject((token, scan.url).some)
        case None =>
          logger.branch("github").info(s"ignoring token ${scan.token}")
          fuccess(none)
  yield res.flatten

  private val accessTokenCache =
    cacheApi[AccessToken.Id, Option[AccessToken.ForAuth]](1024, "oauth.access_token"):
      _.expireAfterWrite(5 minutes).buildAsyncFuture(fetchAccessToken)

  private def fetchAccessToken(id: AccessToken.Id): Fu[Option[AccessToken.ForAuth]] =
    coll.findAndUpdateSimplified[AccessToken.ForAuth](
      selector = $id(id),
      update = $set(F.usedAt -> nowInstant),
      fields = AccessToken.forAuthProjection.some
    )

  private def onRevoke(id: AccessToken.Id): Unit =
    accessTokenCache.put(id, fuccess(none))
    lila.common.Bus.publish(TokenRevoke(id.value), "oauth")

object AccessTokenApi:
  case class Client(origin: String, usedAt: Option[Instant], scopes: List[OAuthScope])

  case class GithubSecretScan(token: Bearer, `type`: String, url: String, source: String)
  given Reads[GithubSecretScan] = Json.reads[GithubSecretScan]
