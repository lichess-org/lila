package lila.pref

import play.api.mvc.RequestHeader

object RequestPref:

  import Pref.default

  def queryParamOverride(req: RequestHeader)(pref: Pref): Pref =
    queryParam(req.queryString, "bg")
      .flatMap(Pref.Bg.fromString.get)
      .fold(pref): bg =>
        pref.copy(bg = bg)

  def fromRequest(req: RequestHeader): Pref =
    val qs = req.queryString
    if qs.isEmpty && req.session.isEmpty then default
    else
      def paramOrSession(name: String): Option[String] = queryParam(qs, name) orElse req.session.get(name)
      default.copy(
        bg = paramOrSession("bg").flatMap(Pref.Bg.fromString.get) | default.bg,
        theme = paramOrSession("theme") | default.theme,
        theme3d = paramOrSession("theme3d") | default.theme3d,
        pieceSet = paramOrSession("pieceSet") | default.pieceSet,
        pieceSet3d = paramOrSession("pieceSet3d") | default.pieceSet3d,
        soundSet = paramOrSession("soundSet") | default.soundSet,
        bgImg = paramOrSession("bgImg"),
        is3d = paramOrSession("is3d") has "true"
      )

  private def queryParam(queryString: Map[String, Seq[String]], name: String): Option[String] =
    queryString
      .get(name)
      .flatMap(_.headOption)
      .filter: v =>
        v.nonEmpty && v != "auto"
