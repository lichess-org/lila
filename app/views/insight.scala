package views.html

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.user.User
import play.api.i18n.Lang

import controllers.routes

object insight:

  def index(
      u: User,
      insightUser: lila.insight.InsightUser,
      prefId: Int,
      ui: play.api.libs.json.JsObject,
      question: play.api.libs.json.JsObject,
      stale: Boolean
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.insight.xChessInsights.txt(u.username),
      moreJs = frag(
        iifeModule("javascripts/insight-refresh.js"),
        jsModuleInit(
          "insight",
          Json.obj(
            "ui"              -> ui,
            "initialQuestion" -> question,
            "i18n"            -> Json.obj(),
            "myUserId"        -> ctx.userId,
            "user" -> (lila.common.LightUser.lightUserWrites.writes(u.light) ++ Json.obj(
              "nbGames" -> insightUser.count,
              "stale"   -> stale,
              "shareId" -> prefId
            )),
            "pageUrl" -> routes.Insight.index(u.username).url,
            "postUrl" -> routes.Insight.json(u.username).url
          )
        )
      ),
      moreCss = cssTag("insight")
    )(
      frag(main(id := "insight"))
    )

  def empty(u: User)(using PageContext) =
    views.html.base.layout(
      title = trans.insight.xChessInsights.txt(u.username),
      moreJs = iifeModule("javascripts/insight-refresh.js"),
      moreCss = cssTag("insight")
    )(
      main(cls := "box box-pad page-small")(
        boxTop(h1(cls := "text", dataIcon := licon.Target)(trans.insight.xChessInsights(u.username))),
        p(trans.insight.xHasNoChessInsights(userLink(u))),
        refreshForm(u, trans.insight.generateInsights.txt(u.username))
      )
    )

  def forbidden(u: User)(using PageContext) =
    views.html.site.message(
      title = trans.insight.insightsAreProtected.txt(u.username),
      back = routes.User.show(u.id).url.some
    )(
      p(trans.insight.cantSeeInsights(userLink(u))),
      br,
      p(
        trans.insight.maybeAskThemToChangeTheir(
          a(cls := "button", href := routes.Pref.form("site"))(trans.insight.insightsSettings.txt())
        )
      )
    )

  def refreshForm(u: User, action: String)(using Lang) =
    postForm(cls := "insight-refresh", st.action := routes.Insight.refresh(u.username))(
      button(dataIcon := licon.Checkmark, cls := "button text")(action),
      div(cls := "crunching none")(
        spinner,
        br,
        p(strong(trans.insight.crunchingData()))
      )
    )
