package lila.app
package ui

import play.api.mvc.RequestHeader
import play.api.i18n.Lang

import lila.api.Nonce

case class EmbedConfig(
    bg: String,
    board: String,
    pieceSet: lila.pref.PieceSet,
    lang: Lang,
    req: RequestHeader,
    nonce: Nonce
)

object EmbedConfig:

  given configLang(using config: EmbedConfig): Lang         = config.lang
  given configReq(using config: EmbedConfig): RequestHeader = config.req

  def apply(req: RequestHeader): EmbedConfig =
    EmbedConfig(
      bg = get("bg", req).filterNot("auto".==) | "system",
      board = lila.pref.Theme(~get("theme", req)).cssClass,
      pieceSet = lila.pref.PieceSet.get(~get("pieceSet", req)),
      lang = lila.i18n.I18nLangPicker(req, none),
      req = req,
      nonce = Nonce.random
    )

  private def get(name: String, req: RequestHeader): Option[String] =
    req.queryString get name flatMap (_.headOption) filter (_.nonEmpty)
