package lila.soclog
package oauth1

case class OAuth1Provider(
  name: String,
  requestTokenUrl: String,
  accessTokenUrl: String,
  authorizationUrl: String,
  consumerKey: String,
  consumerSecret: String)

final class OAuth1Providers(
    twitter: OAuth1Provider) {

  val list = List(twitter)

  def apply(name: String) = list.find(_.name == name)
}
