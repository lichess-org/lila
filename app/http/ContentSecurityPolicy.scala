package lila.app

import lila.api.Nonce

case class ContentSecurityPolicy(
    defaultSrc: List[String],
    connectSrc: List[String],
    styleSrc: List[String],
    frameSrc: List[String],
    workerSrc: List[String],
    imgSrc: List[String],
    scriptSrc: List[String],
    fontSrc: List[String],
    baseUri: List[String]
):

  def withNonce(nonce: Nonce) = copy(scriptSrc = nonce.scriptSrc :: scriptSrc)

  def withLegacyCompatibility = copy(scriptSrc = "'unsafe-inline'" :: scriptSrc)

  def withWebAssembly =
    copy(
      scriptSrc = "'unsafe-eval'" :: scriptSrc
    )

  def withExternalEngine(url: String) = copy(connectSrc = url :: connectSrc)

  def withStripe =
    copy(
      connectSrc = "https://*.stripe.com" :: connectSrc,
      scriptSrc = "https://*.stripe.com" :: scriptSrc,
      frameSrc = "https://*.stripe.com" :: frameSrc
    )

  def withPayPal =
    copy(
      connectSrc = "https://*.paypal.com" :: connectSrc,
      scriptSrc = "https://*.paypal.com" :: scriptSrc,
      frameSrc = "https://*.paypal.com" :: frameSrc
    )

  def finalizeWithTwitch =
    copy(
      defaultSrc = Nil,
      connectSrc = "https://www.twitch.tv" :: "https://www-cdn.jtvnw.net" :: connectSrc,
      styleSrc = Nil,
      frameSrc = Nil,
      workerSrc = Nil,
      scriptSrc = Nil
    )

  def withTwitter =
    copy(
      scriptSrc = "https://platform.twitter.com" :: "https://*.twimg.com" :: scriptSrc,
      frameSrc = "https://twitter.com" :: "https://platform.twitter.com" :: frameSrc,
      styleSrc = "https://platform.twitter.com" :: styleSrc
    )

  def withGoogleForm = copy(frameSrc = "https://docs.google.com" :: frameSrc)

  private val hCaptchaDomains = List("https://hcaptcha.com", "https://*.hcaptcha.com")

  def withHcaptcha =
    copy(
      scriptSrc = hCaptchaDomains ::: scriptSrc,
      frameSrc = hCaptchaDomains ::: frameSrc,
      styleSrc = hCaptchaDomains ::: styleSrc,
      connectSrc = hCaptchaDomains ::: connectSrc
    )

  def withPeer = copy(connectSrc = "wss://0.peerjs.com" :: connectSrc)

  private def withPrismicEditor(maybe: Boolean): ContentSecurityPolicy =
    if maybe then
      copy(
        scriptSrc = "https://static.cdn.prismic.io" :: scriptSrc,
        frameSrc = "https://lichess.prismic.io" :: "https://lichess.cdn.prismic.io" :: frameSrc,
        connectSrc = "https://lichess.prismic.io" :: "https://lichess.cdn.prismic.io" :: connectSrc
      )
    else this

  def withPrismic(editor: Boolean): ContentSecurityPolicy = withPrismicEditor(editor).withTwitter

  def withAnyWs = copy(connectSrc = "ws:" :: "wss:" :: connectSrc)

  def withWikiBooks = copy(connectSrc = "en.wikibooks.org" :: connectSrc)

  def withLilaHttp = copy(connectSrc = "http.lichess.org" :: connectSrc)

  def withInlineIconFont = copy(fontSrc = "data:" :: fontSrc)

  override def toString: String =
    List(
      "default-src " -> defaultSrc,
      "connect-src " -> connectSrc,
      "style-src "   -> styleSrc,
      "frame-src "   -> frameSrc,
      "worker-src "  -> workerSrc,
      "img-src "     -> imgSrc,
      "script-src "  -> scriptSrc,
      "font-src "    -> fontSrc,
      "base-uri "    -> baseUri
    ) collect {
      case (directive, sources) if sources.nonEmpty =>
        sources.mkString(directive, " ", ";")
    } mkString " "
