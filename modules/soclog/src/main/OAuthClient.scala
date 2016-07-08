package lila.soclog

import _root_.oauth.signpost.exception.OAuthException
import play.api.libs.json._
import play.api.libs.json.JsValue
import play.api.libs.oauth.{ OAuth => PlayClient, _ }
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

import lila.common.PimpedJson._

private final class OAuthClient(baseUrl: String) {

  def retrieveRequestToken(provider: Provider): Fu[RequestToken] = withFuture {
    clientOf(provider).retrieveRequestToken(callbackUrlOf(provider))
  }

  def retrieveAccessToken(provider: Provider, token: RequestToken, verifier: String): Fu[AccessToken] = withFuture {
    clientOf(provider).retrieveAccessToken(token, verifier)
  }.map { accessToken =>
    AccessToken(accessToken.token, accessToken.secret)
  }

  def retrieveProfile(provider: Provider, url: String, accessToken: AccessToken): Fu[JsValue] =
    WS.url(url).sign(
      OAuthCalculator(serviceInfoOf(provider).key, RequestToken(accessToken.token, accessToken.secret))
    ).get().map(_.json)

  def redirectUrl(provider: Provider, token: String) = clientOf(provider).redirectUrl(token)

  private def callbackUrlOf(provider: Provider) =
    s"$baseUrl/soclog/${provider.name}/callback"

  private def clientOf(provider: Provider) = PlayClient(
    serviceInfoOf(provider),
    use10a = true)

  private def serviceInfoOf(provider: Provider) = ServiceInfo(
    requestTokenURL = provider.requestTokenUrl,
    accessTokenURL = provider.accessTokenUrl,
    authorizationURL = provider.authorizationUrl,
    key = ConsumerKey(
      provider.consumerKey,
      provider.consumerSecret))

  private def withFuture(call: => Either[OAuthException, RequestToken]): Fu[RequestToken] =
    call match {
      case Left(error)  => fufail(error)
      case Right(token) => fuccess(token)
    }
}
