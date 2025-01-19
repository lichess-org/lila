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
    baseUri: List[String],
) {

  def withNonce(nonce: Nonce) = copy(scriptSrc = nonce.scriptSrc :: scriptSrc)

  def withWebAssembly =
    copy(
      scriptSrc = "'unsafe-eval'" :: scriptSrc,
    )

  def withTwitch =
    copy(
      defaultSrc = Nil,
      connectSrc = "https://www.twitch.tv" :: "https://www-cdn.jtvnw.net" :: connectSrc,
      styleSrc = Nil,
      fontSrc = Nil,
      frameSrc = Nil,
      workerSrc = Nil,
      scriptSrc = Nil,
    )

  def withTwitter =
    copy(
      scriptSrc = "https://platform.twitter.com" :: "https://*.twimg.com" :: scriptSrc,
      frameSrc = "https://twitter.com" :: "https://platform.twitter.com" :: frameSrc,
      styleSrc = "https://platform.twitter.com" :: styleSrc,
    )

  def withRecaptcha =
    copy(
      scriptSrc = "https://www.google.com" :: scriptSrc,
      frameSrc = "https://www.google.com" :: frameSrc,
    )

  def withPeer =
    copy(
      connectSrc = "wss://0.peerjs.com" :: connectSrc,
    )

  override def toString: String =
    List(
      "default-src " -> defaultSrc,
      "connect-src " -> connectSrc,
      "style-src "   -> styleSrc,
      "font-src "    -> fontSrc,
      "frame-src "   -> frameSrc,
      "worker-src "  -> workerSrc,
      "img-src "     -> imgSrc,
      "script-src "  -> scriptSrc,
      "base-uri "    -> baseUri,
    ) collect {
      case (directive, sources) if sources.nonEmpty =>
        sources.mkString(directive, " ", ";")
    } mkString " "
}
