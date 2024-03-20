package views.html

import play.api.libs.json.*

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.*

object msg:

  def home(json: JsObject)(using PageContext) =
    views.html.base.layout(
      moreCss = frag(cssTag("msg")),
      pageModule = PageModule("msg", Json.obj("data" -> json, "i18n" -> i18nJsObject(i18nKeys))).some,
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
