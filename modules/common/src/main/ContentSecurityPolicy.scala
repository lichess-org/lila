package lila.common

case class ContentSecurityPolicy(
    defaultSrc: List[String],
    connectSrc: List[String],
    styleSrc: List[String],
    fontSrc: List[String],
    childSrc: List[String],
    imgSrc: List[String],
    scriptSrc: List[String]
) {

  private def withScriptSrc(source: String) = copy(scriptSrc = source :: scriptSrc)

  def withNonce(nonce: Nonce) = withScriptSrc(nonce.scriptSrc)

  def withStripe = copy(
    connectSrc = "https://*.stripe.com" :: connectSrc,
    scriptSrc = "https://*.stripe.com" :: scriptSrc,
    childSrc = "https://*.stripe.com" :: childSrc
  )

  def withSpreadshirt = copy(
    defaultSrc = Nil,
    connectSrc = "https://shop.spreadshirt.com" :: "https://api.spreadshirt.com" :: connectSrc,
    styleSrc = Nil,
    fontSrc = Nil,
    childSrc = Nil,
    imgSrc = Nil,
    scriptSrc = Nil
  )

  def withTwitch = copy(
    defaultSrc = Nil,
    connectSrc = "https://www.twitch.tv" :: "https://www-cdn.jtvnw.net" :: connectSrc,
    styleSrc = Nil,
    fontSrc = Nil,
    childSrc = Nil,
    imgSrc = Nil,
    scriptSrc = Nil
  )

  def withTwitter = copy(
    scriptSrc = "https://platform.twitter.com" :: "https://*.twimg.com" :: scriptSrc,
    childSrc = "https://platform.twitter.com" :: childSrc,
    styleSrc = "https://platform.twitter.com" :: styleSrc
  )

  override def toString: String =
    List(
      "default-src " -> defaultSrc,
      "connect-src " -> connectSrc,
      "style-src " -> styleSrc,
      "font-src " -> fontSrc,
      "child-src " -> childSrc,
      "img-src " -> imgSrc,
      "script-src " -> scriptSrc
    ) collect {
        case (directive, sources) if sources.nonEmpty =>
          sources.mkString(directive, " ", ";")
      } mkString (" ")
}
