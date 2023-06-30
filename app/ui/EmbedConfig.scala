package lila.app
package ui

import play.api.mvc.RequestHeader
import play.api.i18n.Lang

import lila.common.Nonce

case class EmbedConfig(
    bg: String,
    board: String,
    pieceSet: lila.pref.PieceSet,
    chuPieceSet: lila.pref.PieceSet,
    kyoPieceSet: lila.pref.PieceSet,
    lang: Lang,
    req: RequestHeader,
    nonce: Nonce
)

object EmbedConfig {

  object implicits {
    implicit def configLang(implicit config: EmbedConfig): Lang         = config.lang
    implicit def configReq(implicit config: EmbedConfig): RequestHeader = config.req
  }

  def apply(req: RequestHeader): EmbedConfig = {
    val pieceSet = get("pieceSet", req)
    EmbedConfig(
      bg = get("bg", req).filterNot("auto".==) | "dark",
      board = lila.pref.Theme(~get("theme", req)).cssClass,
      pieceSet = lila.pref.PieceSet(~pieceSet),
      chuPieceSet = lila.pref.ChuPieceSet(get("chuPieceSet", req) | ~pieceSet),
      kyoPieceSet = lila.pref.KyoPieceSet(get("kyoPieceSet", req) | ~pieceSet),
      lang = get("lang", req).flatMap(lila.i18n.I18nLangPicker.byQuery) | lila.i18n.I18nLangPicker(req, none),
      req = req,
      nonce = Nonce.random
    )
  }

  private def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (_.nonEmpty)
}
