package lila.pref

import play.api.mvc.RequestHeader

object RequestPref:

  import Pref.default

  def queryParamOverride(pref: Pref)(using req: RequestHeader): Pref =
    queryParam("bg")
      .flatMap(Pref.Bg.fromString.get)
      .fold(pref): bg =>
        pref.copy(bg = bg)

  def fromRequest(using req: RequestHeader): Pref =
    if req.queryString.isEmpty && req.session.isEmpty then default
    else
      inline def session(inline name: String): Option[String] = req.session.get(name)
      def paramOrSession(name: String): Option[String] = queryParam(name).orElse(session(name))
      default.copy(
        bg = paramOrSession("bg").flatMap(Pref.Bg.fromString.get) | default.bg,
        theme = paramOrSession("theme") | default.theme,
        theme3d = session("theme3d") | default.theme3d,
        pieceSet = paramOrSession("pieceSet") | default.pieceSet,
        pieceSet3d = session("pieceSet3d") | default.pieceSet3d,
        soundSet = session("soundSet") | default.soundSet,
        is3d = session("is3d").has("true"),
        uiRoundness = session("uiRoundness").flatMap(_.toIntOption) | default.uiRoundness,
        board = default.board.copy(
          opacity = session("boardOpacity").flatMap(_.toIntOption) | default.board.opacity,
          brightness = session("boardBrightness").flatMap(_.toIntOption) | default.board.brightness,
          contrast = session("boardContrast").flatMap(_.toIntOption) | default.board.contrast,
          hue = session("boardHue").flatMap(_.toIntOption) | default.board.hue
        )
      )

  private def queryParam(name: String)(using RequestHeader): Option[String] =
    lila.common.HTTPRequest.queryStringGet(name).filter(_ != "auto")
