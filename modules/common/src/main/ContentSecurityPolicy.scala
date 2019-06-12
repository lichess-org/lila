package lila.common

case class ContentSecurityPolicy(
    defaultSrc: List[String],
    connectSrc: List[String],
    styleSrc: List[String],
    fontSrc: List[String],
    frameSrc: List[String],
    workerSrc: List[String],
    imgSrc: List[String],
    scriptSrc: List[String],
    baseUri: List[String]
) {

  private def withScriptSrc(source: String) = copy(scriptSrc = source :: scriptSrc)

  def withNonce(nonce: Nonce) = copy(
    // Nonces are not supported by Safari but 'unsafe-inline' is ignored by
    // better browsers if there are also nonces.
    scriptSrc = nonce.scriptSrc :: "'unsafe-inline'" :: scriptSrc
  )

  def withStripe = copy(
    connectSrc = "https://*.stripe.com" :: connectSrc,
    scriptSrc = "https://*.stripe.com" :: scriptSrc,
    frameSrc = "https://*.stripe.com" :: frameSrc
  )

  def withSpreadshirt = copy(
    defaultSrc = Nil,
    connectSrc = "https://shop.spreadshirt.com" :: "https://api.spreadshirt.com" :: connectSrc,
    styleSrc = Nil,
    fontSrc = Nil,
    frameSrc = Nil,
    workerSrc = Nil,
    imgSrc = Nil,
    scriptSrc = Nil
  )

  def withTwitch = copy(
    defaultSrc = Nil,
    connectSrc = "https://www.twitch.tv" :: "https://www-cdn.jtvnw.net" :: connectSrc,
    styleSrc = Nil,
    fontSrc = Nil,
    frameSrc = Nil,
    workerSrc = Nil,
    imgSrc = Nil,
    scriptSrc = Nil
  )

  def withTwitter = copy(
    scriptSrc = "https://platform.twitter.com" :: "https://*.twimg.com" :: scriptSrc,
    frameSrc = "https://twitter.com" :: "https://platform.twitter.com" :: frameSrc,
    styleSrc = "https://platform.twitter.com" :: styleSrc
  )

  def withGoogleForm = copy(frameSrc = "https://docs.google.com" :: frameSrc)

  def withRecaptcha = copy(
    scriptSrc = "https://www.google.com" :: scriptSrc,
    frameSrc = "https://www.google.com" :: frameSrc
  )

  private def withPrismicEditor(maybe: Boolean): ContentSecurityPolicy = if (maybe) copy(
    scriptSrc = "https://static.cdn.prismic.io" :: scriptSrc,
    frameSrc = "https://lichess.prismic.io" :: "https://lichess.cdn.prismic.io" :: frameSrc,
    connectSrc = "https://lichess.prismic.io" :: "https://lichess.cdn.prismic.io" :: connectSrc
  )
  else this

  def withPrismic(editor: Boolean): ContentSecurityPolicy = withPrismicEditor(editor).withTwitter

  override def toString: String = List(
    "default-src " -> defaultSrc,
    "connect-src " -> connectSrc,
    "style-src " -> styleSrc,
    "font-src " -> fontSrc,
    "frame-src " -> frameSrc,
    "worker-src " -> workerSrc,
    "img-src " -> imgSrc,
    "script-src " -> scriptSrc,
    "base-uri " -> baseUri
  ) collect {
      case (directive, sources) if sources.nonEmpty =>
        sources.mkString(directive, " ", ";")
    } mkString " "
}
