package views.html

import play.api.libs.json.*

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue

object msg:

  def home(json: JsObject)(using Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("msg")),
      moreJs = frag(
        jsModule("msg"),
        embedJsUnsafeLoadThen(
          s"""LichessMsg(${safeJsonValue(
              Json.obj(
                "data" -> json,
                "i18n" -> i18nJsObject(i18nKeys)
              )
            )})"""
        )
      ),
      title = trans.inbox.txt(),
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "box msg-app")
    }

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
    trans.yesterday,
    trans.youAreLeavingLichess,
    trans.neverTypeYourPassword,
    trans.cancel,
    trans.proceedToX
  )
