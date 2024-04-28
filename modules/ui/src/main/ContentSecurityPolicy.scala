package lila.ui

import lila.core.config.AssetDomain

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
  def withNonce(nonce: Nonce) = copy(scriptSrc = s"'nonce-${nonce}'" :: scriptSrc)

  def withLegacyCompatibility = copy(scriptSrc = "'unsafe-inline'" :: scriptSrc)

  def withWebAssembly = copy(scriptSrc = "'unsafe-eval'" :: scriptSrc)

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

  def withAnyWs = copy(connectSrc = "ws:" :: "wss:" :: connectSrc)

  def withWikiBooks = copy(connectSrc = "en.wikibooks.org" :: connectSrc)

  // for extensions to use their cloud eval API
  // https://www.chessdb.cn/cloudbook_api_en.html
  def withChessDbCn = copy(connectSrc = "www.chessdb.cn" :: connectSrc)

  def withExternalAnalysisApis = withWikiBooks.withChessDbCn

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
    ).collect {
      case (directive, sources) if sources.nonEmpty =>
        sources.mkString(directive, " ", ";")
    }.mkString(" ")

object ContentSecurityPolicy:

  def basic(assetDomain: AssetDomain, connectSrcs: List[String]) = ContentSecurityPolicy(
    defaultSrc = List("'self'", assetDomain.value),
    connectSrc = "'self'" :: "blob:" :: "data:" :: connectSrcs,
    styleSrc = List("'self'", "'unsafe-inline'", assetDomain.value),
    frameSrc = List("'self'", assetDomain.value, "www.youtube.com", "player.twitch.tv"),
    workerSrc = List("'self'", assetDomain.value, "blob:"),
    imgSrc = List("'self'", "blob:", "data:", "*"),
    scriptSrc = List("'self'", assetDomain.value),
    fontSrc = List("'self'", assetDomain.value),
    baseUri = List("'none'")
  )
