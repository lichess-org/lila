package views.html

import play.api.libs.json.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue

object msg:

  def home(json: JsObject)(using PageContext) =
    views.html.base.layout(
      moreCss = frag(cssTag("msg")),
      moreJs = jsModuleInit("msg", Json.obj("data" -> json, "i18n" -> i18nJsObject(i18nKeys))),
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
