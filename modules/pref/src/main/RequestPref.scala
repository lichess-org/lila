package lila.pref

import play.api.mvc.RequestHeader

object RequestPref {

  import Pref.default

  def queryParamOverride(req: RequestHeader)(pref: Pref): Pref =
    queryParam(req, "bg").fold(pref) { bg =>
      pref.copy(
        dark = bg != "light",
        transp = bg == "transp"
      )
    }

  def fromRequest(req: RequestHeader): Pref = {

    def paramOrSession(name: String): Option[String] =
      queryParam(req, name) orElse req.session.get(name)

    val bg = paramOrSession("bg") | "light"

    default.copy(
      dark = bg != "light",
      transp = bg == "transp",
      theme = paramOrSession("theme") | default.theme,
      theme3d = req.session.data.getOrElse("theme3d", default.theme3d),
      pieceSet = req.session.data.getOrElse("pieceSet", default.pieceSet),
      pieceSet3d = req.session.data.getOrElse("pieceSet3d", default.pieceSet3d),
      soundSet = req.session.data.getOrElse("soundSet", default.soundSet),
      bgImg = req.session.data.get("bgImg"),
      is3d = req.session.data.get("is3d") has "true",
      pieceNotation = (req.session.data.get("pieceNotation").getOrElse("0")).toInt
    )
  }

  private def queryParam(req: RequestHeader, name: String): Option[String] =
    req.queryString.get(name).flatMap(_.headOption).filter { v =>
      v.nonEmpty && v != "auto"
    }
}
