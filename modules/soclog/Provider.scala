package lila.soclog

case class Provider(
  name: String,
  requestTokenUrl: String,
  accessTokenUrl: String,
  authorizationUrl: String,
  consumerKey: String,
  consumerSecret: String)

final class Providers(
    twitter: Provider) {

  val list = List(twitter)

  def apply(name: String) = list.find(_.name == name)
}

private object Profiler {

  type FillProfile = OAuthClient => OAuth1Info => Fu[Profile]

  def apply(provider: Provider): FillProfile = provider.name match {
    case "twitter" => twitter(provider)
    case x         => sys error s"No such provider $x"
  }

  val twitter: Provider => FillProfile = provider => client => info =>
    client.retrieveProfile(provider, "https://api.twitter.com/1.1/account/verify_credentials.json", info).map { me =>
      val userId = (me \ "id_str").as[String]
      val name = (me \ "name").asOpt[String]
      Profile(
        providerId = "twitter",
        userId = userId,
        fullName = name,
        oAuth1Info = Some(info))
    }
}
