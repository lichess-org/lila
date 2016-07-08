package lila.soclog
package oauth1

import org.joda.time.DateTime
import play.api.libs.oauth.RequestToken
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.UserRepo

final class OAuth1Api(client: OAuth1Client, coll: Coll) {

  import OAuth1.BSONHandlers._

  private val requestTokenStorage = lila.memo.Builder.expiry[String, RequestToken](10 minutes)

  def start(provider: OAuth1Provider)(implicit request: RequestHeader): Fu[Result] =
    client.retrieveRequestToken(provider) map { requestToken =>
      val cacheKey = randomId
      requestTokenStorage.put(cacheKey, requestToken)
      Redirect {
        client.redirectUrl(provider, requestToken.token)
      } withCookies lila.common.LilaCookie.withSession { session =>
        session + (OAuth1Api.cacheKey -> cacheKey)
      }
    }

  def finish(provider: OAuth1Provider)(implicit request: RequestHeader): Fu[OAuth1Result] =
    getOAuth(provider).fold[Fu[OAuth1Result]](fuccess(OAuth1Result.Nope)) {
      _ flatMap { oauth =>
        UserRepo bySoclog oauth.id map {
          case Some(user) => OAuth1Result.Authenticated(user)
          case None       => OAuth1Result.PickUsername(oauth)
        }
      }
    }

  def findOAuth(oAuthId: String) = coll.uno[OAuth1]($id(oAuthId))

  private def getOAuth(provider: OAuth1Provider)(implicit request: RequestHeader): Option[Fu[OAuth1]] = for {
    cacheKey <- request.session.get(OAuth1Api.cacheKey)
    requestToken <- Option(requestTokenStorage getIfPresent cacheKey)
    verifier <- request.queryString.get("oauth_verifier").map(_.head)
  } yield client.retrieveAccessToken(provider, requestToken, verifier) flatMap { tokens =>
    OAuth1Profiler(provider)(client)(tokens) flatMap findOrCreateOAuth(tokens, provider)
  }

  private def findOrCreateOAuth(tokens: OAuth1AccessToken, provider: OAuth1Provider)(profile: Profile): Fu[OAuth1] =
    coll.uno[OAuth1]($doc(
      "provider" -> provider.name,
      "profile.userId" -> profile.userId
    )) flatMap {
      case Some(oauth) => coll.updateField($id(oauth.id), "updatedAt", DateTime.now) inject oauth
      case None =>
        val oauth = OAuth1.make(provider, profile, tokens)
        coll insert oauth inject oauth
    }

  private def randomId = ornicar.scalalib.Random nextStringUppercase 10
}

object OAuth1Api {

  val cacheKey = "oauth1_cache_key"
}
