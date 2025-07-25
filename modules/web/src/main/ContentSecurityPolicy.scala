package lila.web

import lila.core.config.AssetDomain

object ContentSecurityPolicy:

  def page(assetDomain: AssetDomain, connectSrcs: List[String]) =
    lila.ui.ContentSecurityPolicy(
      defaultSrc = List("'self'", assetDomain.value),
      connectSrc = "'self'" :: "blob:" :: "data:" :: connectSrcs,
      styleSrc = List("'self'", "'unsafe-inline'", assetDomain.value),
      frameSrc = List("'self'", assetDomain.value, "www.youtube.com", "player.twitch.tv", "player.vimeo.com"),
      workerSrc = List("'self'", assetDomain.value, "blob:"),
      imgSrc = List("'self'", "blob:", "data:", "*"),
      mediaSrc = List("'self'", "blob:", assetDomain.value),
      scriptSrc = List("'self'", assetDomain.value),
      fontSrc = List("'self'", assetDomain.value),
      baseUri = List("'none'")
    )

  def embed(assetDomain: AssetDomain) =
    lila.ui.ContentSecurityPolicy(
      defaultSrc = List("'self'", assetDomain.value),
      connectSrc = List("'self'", "blob:", "data:", assetDomain.value),
      styleSrc = List("'self'", "'unsafe-inline'", assetDomain.value),
      frameSrc = Nil,
      workerSrc = Nil,
      imgSrc = List("'self'", "blob:", "data:", "*"),
      mediaSrc = Nil,
      scriptSrc = List("'self'", assetDomain.value),
      fontSrc = List("'self'", assetDomain.value),
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
