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

  def fromRequest(req: RequestHeader, languageNotation: Boolean = true): Pref = {

    def paramOrSession(name: String): Option[String] =
      queryParam(req, name) orElse req.session.get(name)

    val customTheme = CustomTheme(
      boardColor = paramOrSession("boardColor") | CustomTheme.default.boardColor,
      boardImg = paramOrSession("boardImg") | CustomTheme.default.boardImg,
      gridColor = paramOrSession("gridColor") | CustomTheme.default.gridColor,
      gridWidth = paramOrSession("gridWidth").flatMap(_.toIntOption) | CustomTheme.default.gridWidth,
      handsColor = paramOrSession("handsColor") | CustomTheme.default.handsColor,
      handsImg = paramOrSession("handsImg") | CustomTheme.default.handsImg
    ).some.filterNot(_ == CustomTheme.default)
    val bg       = paramOrSession("bg") | "dark"
    val theme    = paramOrSession("theme") | default.theme
    val pieceSet = paramOrSession("pieceSet")

    default.copy(
      dark = bg != "light",
      transp = bg == "transp",
      theme = theme,
      pieceSet = pieceSet | default.pieceSet,
      chuPieceSet = paramOrSession("chuPieceSet") | default.chuPieceSet,
      kyoPieceSet = paramOrSession("kyoPieceSet") | default.kyoPieceSet,
      soundSet = paramOrSession("soundSet") | default.soundSet,
      bgImg = paramOrSession("bgImg"),
      notation = paramOrSession("notation").flatMap(_.toIntOption) | defaultNotation(req, languageNotation),
      thickGrid = paramOrSession("thickGrid").flatMap(_.toIntOption) | default.thickGrid,
      customTheme = customTheme
    )
  }

  private def queryParam(req: RequestHeader, name: String): Option[String] =
    req.queryString.get(name).flatMap(_.headOption).filter { v =>
      v.nonEmpty && v != "auto"
    }

  private def defaultNotation(req: RequestHeader, languageNotation: Boolean): Int =
    if (languageNotation && req.acceptLanguages.headOption.exists(_.language == "ja"))
      Notations.japanese.index
    else default.notation

}
