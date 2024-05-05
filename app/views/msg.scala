package views.msg

import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }

def home(json: JsObject)(using Context) =
  Page(trans.site.inbox.txt())
    .css("msg")
    .js(PageModule("msg", Json.obj("data" -> json, "i18n" -> i18nJsObject(i18nKeys))))
    .csp(_.withInlineIconFont):
      main(cls := "box msg-app")

private val i18nKeys = List(
  trans.site.inbox,
  trans.challenge.challengeToPlay,
  trans.site.block,
  trans.site.unblock,
  trans.site.blocked,
  trans.site.delete,
  trans.site.reportXToModerators,
  trans.site.searchOrStartNewDiscussion,
  trans.site.players,
  trans.site.friends,
  trans.site.discussions,
  trans.site.today,
  trans.site.yesterday,
  trans.site.youAreLeavingLichess,
  trans.site.neverTypeYourPassword,
  trans.site.cancel,
  trans.site.proceedToX
)
