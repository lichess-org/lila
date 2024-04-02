package views.html

import play.api.libs.json.*

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.*

object msg:

  def home(json: JsObject)(using PageContext) =
    views.html.base.layout(
      moreCss = frag(cssTag("msg")),
      pageModule = PageModule("msg", Json.obj("data" -> json, "i18n" -> i18nJsObject(i18nKeys))).some,
      title = trans.site.inbox.txt(),
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "box msg-app")
    }

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
