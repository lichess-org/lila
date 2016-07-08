package lila.soclog
package oauth1

import _root_.oauth.signpost.exception.OAuthException
import play.api.libs.json._
import play.api.libs.json.JsValue
import play.api.libs.oauth.{ OAuth => PlayClient, _ }
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

import lila.common.PimpedJson._

private[soclog] final class OAuth1Client(callbackUrl: OAuth1Provider => String) {

  def retrieveRequestToken(provider: OAuth1Provider): Fu[RequestToken] = withFuture {
    clientOf(provider).retrieveRequestToken(callbackUrl(provider))
  }

  def retrieveAccessToken(provider: OAuth1Provider, token: RequestToken, verifier: String): Fu[OAuth1AccessToken] = withFuture {
    clientOf(provider).retrieveAccessToken(token, verifier)
  }.map { t =>
    OAuth1AccessToken(t.token, t.secret)
  }

  def retrieveProfile(provider: OAuth1Provider, url: String, accessToken: OAuth1AccessToken): Fu[JsValue] =
    WS.url(url).sign(
      OAuthCalculator(serviceInfoOf(provider).key, RequestToken(accessToken.token, accessToken.secret))
    ).get().map(_.json)

  def redirectUrl(provider: OAuth1Provider, token: String) = clientOf(provider).redirectUrl(token)

  private def clientOf(provider: OAuth1Provider) = PlayClient(
    serviceInfoOf(provider),
    use10a = true)

  private def serviceInfoOf(provider: OAuth1Provider) = ServiceInfo(
    requestTokenURL = provider.requestTokenUrl,
    accessTokenURL = provider.accessTokenUrl,
    authorizationURL = provider.authorizationUrl,
    key = ConsumerKey(
      provider.consumerKey,
      provider.consumerSecret))

  private def withFuture(call: => Either[OAuthException, RequestToken]): Fu[RequestToken] =
    call match {
      case Left(error) =>
        println(error.getCause)
        println(error.getMessage)
        fufail(error)
      case Right(token) => fuccess(token)
    }
}
