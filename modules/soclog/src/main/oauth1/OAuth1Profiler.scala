package lila.soclog
package oauth1

private object OAuth1Profiler {

  type FillProfile = OAuth1Client => OAuth1AccessToken => Fu[Profile]

  def apply(provider: OAuth1Provider): FillProfile = provider.name match {
    case "twitter" => twitter(provider)
    case x         => sys error s"No such provider $x"
  }

  val twitter: OAuth1Provider => FillProfile = provider => client => accessToken =>
    client.retrieveProfile(provider, "https://api.twitter.com/1.1/account/verify_credentials.json?skip_status=true", accessToken).map { me =>
      Profile(
        userId = (me \ "id_str").as[String],
        username = (me \ "screen_name").as[String],
        fullName = (me \ "name").asOpt[String])
    }
}
