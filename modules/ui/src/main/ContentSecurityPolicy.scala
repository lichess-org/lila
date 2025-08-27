package lila.ui

case class ContentSecurityPolicy(
    defaultSrc: List[String],
    connectSrc: List[String],
    styleSrc: List[String],
    frameSrc: List[String],
    workerSrc: List[String],
    imgSrc: List[String],
    mediaSrc: List[String],
    scriptSrc: List[String],
    fontSrc: List[String],
    baseUri: List[String]
):
  def withNonce(nonce: Nonce) = copy(scriptSrc = s"'nonce-${nonce}'" :: scriptSrc)

  def withWebAssembly = copy(scriptSrc = "'unsafe-eval'" :: scriptSrc)

  def withUnsafeInlineScripts = copy(scriptSrc = "'wasm-unsafe-inline'" :: scriptSrc)

  def withLegacyUnsafeInlineScripts = copy(scriptSrc = "'unsafe-inline'" :: scriptSrc)

  def withExternalEngine(url: String) = copy(connectSrc = url :: connectSrc)

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
