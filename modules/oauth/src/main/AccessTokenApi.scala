package lila.oauth

import reactivemongo.api.bson.*

import lila.common.Bearer
import lila.db.dsl.{ *, given }
import lila.user.{ User, UserRepo }
import lila.hub.actorApi.oauth.TokenRevoke

final class AccessTokenApi(
    coll: Coll,
    cacheApi: lila.memo.CacheApi,
    userRepo: UserRepo
)(using Executor):

  import OAuthScope.given
  import AccessToken.{ BSONFields as F, given }

  private def create(token: AccessToken): Fu[AccessToken] = coll.insert.one(token).inject(token)

  def create(setup: OAuthTokenForm.Data, me: User, isStudent: Boolean): Fu[AccessToken] =
    (fuccess(isStudent) >>| userRepo.isManaged(me.id)) flatMap { noBot =>
      val plain = Bearer.randomPersonal()
      create:
        AccessToken(
          id = AccessToken.Id.from(plain),
          plain = plain,
          userId = me.id,
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
    create:
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
    userRepo
      .enabledByIds(setup.usernames)
      .flatMap: users =>
        val scope = OAuthScope.Challenge.Write
        users
          .traverse: user =>
            coll
              .one[AccessToken]:
                $doc(
                  F.userId       -> user.id,
                  F.clientOrigin -> setup.description,
                  F.scopes       -> scope.key
                )
              .getOrElse:
                val plain = Bearer.randomPersonal()
                create:
                  AccessToken(
                    id = AccessToken.Id.from(plain),
                    plain = plain,
                    userId = user.id,
                    description = s"Challenge admin: ${admin.username}".some,
                    createdAt = nowInstant.some,
                    scopes = TokenScopes(List(scope)),
                    clientOrigin = setup.description.some,
                    expires = Some(nowInstant plusMonths 6)
                  )
              .map(user.id -> _)
          .map(_.toMap)

  def listPersonal(user: User): Fu[List[AccessToken]] =
    coll
      .find:
        $doc(
          F.userId       -> user.id,
          F.clientOrigin -> $exists(false)
        )
      .sort($sort desc F.createdAt) // c.f. isBrandNew
      .cursor[AccessToken]()
      .list(100)

  def usedBoardApi(user: User): Fu[List[AccessToken]] =
    coll
      .find:
        $doc(
          F.scopes -> OAuthScope.Board.Play.key,
          F.usedAt $exists true,
          F.userId -> user.id
        )
      .sort($sort desc F.createdAt)
      .cursor[AccessToken]()
      .list(30)

  def countPersonal(user: User): Fu[Int] =
    coll.countSel:
      $doc(
        F.userId       -> user.id,
        F.clientOrigin -> $exists(false)
      )

  def findCompatiblePersonal(user: User, scopes: OAuthScopes): Fu[Option[AccessToken]] =
    coll.one[AccessToken]:
      $doc(
        F.userId       -> user.id,
        F.clientOrigin -> $exists(false),
        F.scopes $all scopes.value
      )

  def listClients(user: User, limit: Int): Fu[List[AccessTokenApi.Client]] =
    coll
      .aggregateList(limit): framework =>
        import framework.*
        Match(
          $doc(
            F.userId       -> user.id,
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

  def revokeById(id: AccessToken.Id, user: User): Funit =
    coll.delete
      .one:
        $doc(
          F.id     -> id,
          F.userId -> user.id
        )
      .void andDo onRevoke(id)

  def revokeByClientOrigin(clientOrigin: String, user: User): Funit =
    coll
      .find(
        $doc(
          F.userId       -> user.id,
          F.clientOrigin -> clientOrigin
        ),
        $doc(F.id -> 1).some
      )
      .sort($sort desc F.usedAt)
      .cursor[Bdoc]()
      .list(100)
      .flatMap: invalidate =>
        coll.delete
          .one:
            $doc(
              F.userId       -> user.id,
              F.clientOrigin -> clientOrigin
            )
          .map(_ => invalidate.flatMap(_.getAsOpt[AccessToken.Id](F.id)).foreach(onRevoke))

  def revoke(bearer: Bearer) =
    val id = AccessToken.Id from bearer
    coll.delete.one($id(id)) andDo onRevoke(id)

  private[oauth] def get(bearer: Bearer) = accessTokenCache.get(AccessToken.Id.from(bearer))

  def test(bearers: List[Bearer]): Fu[Map[Bearer, Option[AccessToken]]] =
    coll
      .optionsByOrderedIds[AccessToken, AccessToken.Id](
        bearers map AccessToken.Id.from,
        readPref = _.sec
      )(_.id)
      .flatMap: tokens =>
        userRepo.filterDisabled(tokens.flatten.map(_.userId)) map { closedUserIds =>
          val openTokens = tokens.map(_.filter(token => !closedUserIds(token.userId)))
          bearers.zip(openTokens).toMap
        }

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
