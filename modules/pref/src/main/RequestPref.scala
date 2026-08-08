package lila.pref

import monocle.syntax.all.*
import play.api.mvc.RequestHeader

object RequestPref:

  import Pref.default

  def queryParamOverride(pref: Pref)(using req: RequestHeader): Pref =
    val queryPref = queryParam("bg")
      .flatMap(Pref.Bg.fromString.get)
      .fold(pref): bg =>
        pref.copy(bg = bg)
    if queryPref.bg == Pref.Bg.DARKBOARD then
      queryPref.copy(bg = Pref.Bg.DARK).focus(_.board.brightness).replace(60)
    else queryPref // we can remove this darkboard hack with a db migration script

  def fromRequest(using req: RequestHeader): Pref =
    if req.queryString.isEmpty && req.session.isEmpty then default
    else
      def paramOrSession(name: String): Option[String] = queryParam(name).orElse(req.session.get(name))
      default.copy(
        bg = paramOrSession("bg").flatMap(Pref.Bg.fromString.get) | default.bg,
        theme = paramOrSession("theme") | default.theme,
        theme3d = paramOrSession("theme3d") | default.theme3d,
        pieceSet = paramOrSession("pieceSet") | default.pieceSet,
        pieceSet3d = paramOrSession("pieceSet3d") | default.pieceSet3d,
        soundSet = paramOrSession("soundSet") | default.soundSet,
        is3d = paramOrSession("is3d").has("true"),
        uiRoundness = paramOrSession("uiRoundness").flatMap(_.toIntOption) | default.uiRoundness,
        board = default.board.copy(
          opacity = paramOrSession("boardOpacity").flatMap(_.toIntOption) | default.board.opacity,
          brightness = paramOrSession("boardBrightness").flatMap(_.toIntOption) | default.board.brightness,
          contrast = paramOrSession("boardContrast").flatMap(_.toIntOption) | default.board.contrast,
          hue = paramOrSession("boardHue").flatMap(_.toIntOption) | default.board.hue
        )
      )

  private def queryParam(name: String)(using RequestHeader): Option[String] =
    lila.common.HTTPRequest.queryStringGet(name).filter(_ != "auto")
