package lila.web

object ContentSecurityPolicy:

  def page(connectSrcs: List[String]) =
    lila.ui.ContentSecurityPolicy(
      defaultSrc = List("'self'"),
      connectSrc = "'self'" :: "blob:" :: "data:" :: connectSrcs,
      styleSrc = List("'self'", "'unsafe-inline'"),
      frameSrc = List(
        "'self'",
        "www.youtube.com",
        "www.youtube-nocookie.com",
        "player.twitch.tv",
        "player.vimeo.com"
      ),
      workerSrc = List("'self'", "blob:"),
      imgSrc = List("'self'", "blob:", "data:", "*"),
      mediaSrc = List("'self'", "blob:"),
      scriptSrc = List("'self'"),
      fontSrc = List("'self'"),
      baseUri = List("'none'")
    )

  val embed = lila.ui.ContentSecurityPolicy(
    defaultSrc = List("'self'"),
    connectSrc = List("'self'", "blob:", "data:"),
    styleSrc = List("'self'", "'unsafe-inline'"),
    frameSrc = Nil,
    workerSrc = Nil,
    imgSrc = List("'self'", "blob:", "data:", "*"),
    mediaSrc = Nil,
    scriptSrc = List("'self'"),
    fontSrc = List("'self'"),
    baseUri = List("'none'")
  )

  def render(csp: lila.ui.ContentSecurityPolicy): String =
    import csp.*
    List(
      "default-src " -> defaultSrc,
      "connect-src " -> connectSrc,
      "style-src " -> styleSrc,
      "frame-src " -> frameSrc,
      "worker-src " -> workerSrc,
      "img-src " -> imgSrc,
      "media-src " -> mediaSrc,
      "script-src " -> scriptSrc,
      "font-src " -> fontSrc,
      "base-uri " -> baseUri
    ).collect {
      case (directive, sources) if sources.nonEmpty =>
        sources.mkString(directive, " ", ";")
    }.mkString(" ")
