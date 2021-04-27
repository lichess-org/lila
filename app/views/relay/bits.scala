package views.html.relay

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  def howToUse(implicit ctx: Context) =
    a(dataIcon := "", cls := "text", href := routes.Page.loneBookmark("broadcasts"))(
      "How to use Lichess Broadcasts"
    )

  def jsI18n(implicit ctx: Context) =
    views.html.study.jsI18n() ++
      i18nJsObject(List(trans.broadcast.addRound.key))
}
