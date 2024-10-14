package views.msg

import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }

def home(json: JsObject)(using Context) =
  Page(trans.site.inbox.txt())
    .css("msg")
    .i18n("challenge")
    .js(PageModule("msg", Json.obj("data" -> json)))
    .csp(_.withInlineIconFont):
      main(cls := "box msg-app")
