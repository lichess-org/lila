package views.html

import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object msg {

  def home(json: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("msg")),
      moreJs = frag(
        jsModule("msg"),
        embedJsUnsafeLoadThen(
          s"""LichessMsg(${safeJsonValue(
            Json.obj(
              "data" -> json,
              "i18n" -> jsI18n
            )
          )})"""
        )
      ),
      title = "Lichess Inbox"
    ) {
      main(cls := "box msg-app")
    }

  def jsI18n(implicit ctx: Context) = i18nJsObject(i18nKeys)

  private val i18nKeys = List(
    trans.inbox,
    trans.challenge.challengeToPlay,
    trans.block,
    trans.unblock,
    trans.blocked,
    trans.delete,
    trans.reportXToModerators,
    trans.searchOrStartNewDiscussion,
    trans.players,
    trans.friends,
    trans.discussions,
    trans.today,
    trans.yesterday
  ).map(_.key)
}
