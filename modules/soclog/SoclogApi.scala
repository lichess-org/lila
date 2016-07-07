package lila.soclog

import play.api.libs.oauth.RequestToken
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import scala.concurrent.duration._

final class SoclogApi(client: OAuthClient) {

  private val requestTokenStorage = lila.memo.Builder.expiry[String, RequestToken](10 minutes)

  def start(provider: Provider)(implicit request: RequestHeader): Fu[Result] =
    client.retrieveRequestToken(provider) map { requestToken =>
      val cacheKey = randomId
      requestTokenStorage.put(cacheKey, requestToken)
      Redirect {
        client.redirectUrl(provider, requestToken.token)
      }.withSession(request.session + (SoclogApi.cacheKey -> cacheKey))
    }

  def finish(provider: Provider)(implicit request: RequestHeader): Option[Fu[Profile]] = for {
    cacheKey <- request.session.get(SoclogApi.cacheKey)
    requestToken <- Option(requestTokenStorage getIfPresent cacheKey)
    verifier <- request.queryString.get("oauth_verifier").map(_.head)
  } yield client.retrieveOAuth1Info(
    provider,
    RequestToken(requestToken.token, requestToken.secret),
    verifier
  ) flatMap Profiler(provider)(client)

  private def randomId = ornicar.scalalib.Random nextStringUppercase 10
}

private object SoclogApi {

  val cacheKey = "oauth_cache_key"
}
