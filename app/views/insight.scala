package views.insight
import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given

def index(
    u: User,
    insightUser: lila.insight.InsightUser,
    prefId: Int,
    ui: play.api.libs.json.JsObject,
    question: play.api.libs.json.JsObject,
    stale: Boolean
)(using ctx: Context) =
  Page(trans.insight.xChessInsights.txt(u.username))
    .js(Esm("insight.refresh"))
    .js(
      PageModule(
        "insight",
        Json.obj(
          "ui" -> ui,
          "initialQuestion" -> question,
          "myUserId" -> ctx.userId,
          "user" -> (lila.common.Json.lightUser.write(u.light) ++ Json.obj(
            "nbGames" -> insightUser.count,
            "stale" -> stale,
            "shareId" -> prefId
          )),
          "pageUrl" -> routes.Insight.index(u.username).url,
          "postUrl" -> routes.Insight.json(u.username).url
        )
      )
    )
    .css("insight")(main(id := "insight"))

def empty(u: User)(using Context) =
  Page(trans.insight.xChessInsights.txt(u.username))
    .js(Esm("insight.refresh"))
    .css("insight"):
      main(cls := "box box-pad page-small")(
        boxTop(h1(cls := "text", dataIcon := Icon.Target)(trans.insight.xChessInsights(u.username))),
        p(trans.insight.xHasNoChessInsights(userLink(u))),
        refreshForm(u, trans.insight.generateInsights.txt(u.username))
      )

def forbidden(u: User)(using Context) =
  views.site.message(
    title = trans.insight.insightsAreProtected.txt(u.username),
    back = routes.User.show(u.id).url.some
  ):
    frag(
      p(trans.insight.cantSeeInsights(userLink(u))),
      br,
      p(
        trans.insight.maybeAskThemToChangeTheir(
          a(cls := "button", href := routes.Pref.form("site"))(trans.insight.insightsSettings.txt())
        )
      )
    )

def refreshForm(u: User, action: String)(using Translate) =
  postForm(cls := "insight-refresh", st.action := routes.Insight.refresh(u.username))(
    button(dataIcon := Icon.Checkmark, cls := "button text")(action),
    div(cls := "crunching none")(
      spinner,
      br,
      p(strong(trans.insight.crunchingData()))
    )
  )
