package lila.soclog

import org.joda.time.DateTime
import play.api.libs.oauth.RequestToken
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.UserRepo

final class SoclogApi(client: OAuthClient, coll: Coll) {

  import OAuth.BSONHandlers._

  private val requestTokenStorage = lila.memo.Builder.expiry[String, RequestToken](10 minutes)

  def start(provider: Provider)(implicit request: RequestHeader): Fu[Result] =
    client.retrieveRequestToken(provider) map { requestToken =>
      val cacheKey = randomId
      requestTokenStorage.put(cacheKey, requestToken)
      Redirect {
        client.redirectUrl(provider, requestToken.token)
      } withCookies lila.common.LilaCookie.withSession { session =>
        session + (SoclogApi.cacheKey -> cacheKey)
      }
    }

  def finish(provider: Provider)(implicit request: RequestHeader): Fu[AuthResult] =
    getOAuth(provider).fold[Fu[AuthResult]](fuccess(AuthResult.BadRequest)) {
      _ flatMap { oauth =>
        UserRepo bySoclog oauth.id flatMap {
          case Some(user) => fuccess(AuthResult.Authenticated(user))
          case None       => signUp(oauth)
        }
      }
    }

  private def signUp(oauth: OAuth): Fu[SignUpResult] =
    UserRepo named oauth.extId flatMap {
      case Some(existing) => fuccess(SignUpResult.ExistingUsername(oauth, existing))
      case None => UserRepo.createSoclog(oauth.extId, oauth.id).map {
        _.fold[SignUpResult](SignUpResult.Failed)(SignUpResult.SignedUp)
      }
    }

  private def getOAuth(provider: Provider)(implicit request: RequestHeader): Option[Fu[OAuth]] = for {
    cacheKey <- request.session.get(SoclogApi.cacheKey)
    requestToken <- Option(requestTokenStorage getIfPresent cacheKey)
    verifier <- request.queryString.get("oauth_verifier").map(_.head)
  } yield client.retrieveAccessToken(provider, requestToken, verifier) flatMap { tokens =>
    Profiler(provider)(client)(tokens) flatMap findOrCreateOAuth(tokens, provider)
  }

  private def findOrCreateOAuth(tokens: AccessToken, provider: Provider)(profile: Profile): Fu[OAuth] =
    coll.uno[OAuth]($id(OAuth.makeId(provider, profile))) flatMap {
      case Some(oauth) => coll.updateField($id(oauth.id), "updatedAt", DateTime.now) inject oauth
      case None =>
        val oauth = OAuth.make(provider, profile, tokens)
        coll insert oauth inject oauth
    }

  private def randomId = ornicar.scalalib.Random nextStringUppercase 10
}

object SoclogApi {

  val cacheKey = "oauth_cache_key"
}
