package lila.soclog
package oauth2

case class OAuth2Provider(
  name: String,
  authorizationUrl: String,
  accessTokenUrl: String,
  clientId: String,
  clientSecret: String,
  scope: String)

final class OAuth2Providers(
    google: OAuth2Provider) {

  val list = List(twitter)

  def apply(name: String) = list.find(_.name == name)
}
