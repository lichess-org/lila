package views.html

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.user.User

import controllers.routes

object insight {

  def index(
      u: User,
      cache: lila.insight.InsightUser,
      prefId: Int,
      ui: play.api.libs.json.JsObject,
      question: play.api.libs.json.JsObject,
      stale: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s chess insights",
      moreJs = frag(
        highchartsLatestTag,
        jsAt("javascripts/vendor/jquery.min.js"),
        jsAt("javascripts/vendor/multiple-select.min.js"),
        jsModule("insight"),
        jsTag("insight-refresh.js"),
        embedJsUnsafeLoadThen(
          s"""lichess.insight=LichessInsight(document.getElementById('insight'), ${safeJsonValue(
            Json.obj(
              "ui"              -> ui,
              "initialQuestion" -> question,
              "i18n"            -> Json.obj(),
              "myUserId"        -> ctx.userId,
              "user" -> Json.obj(
                "id"      -> u.id,
                "name"    -> u.username,
                "nbGames" -> cache.count,
                "stale"   -> stale,
                "shareId" -> prefId
              ),
              "pageUrl" -> routes.Insight.index(u.username).url,
              "postUrl" -> routes.Insight.json(u.username).url
            )
          )})"""
        )
      ),
      moreCss = cssTag("insight")
    )(
      frag(
        main(id := "insight"),
        stale option div(cls := "insight-stale none")(
          p("There are new games to learn from!"),
          refreshForm(u, "Update insights")
        )
      )
    )

  def empty(u: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s chess insights",
      moreJs = jsTag("insight-refresh.js"),
      moreCss = cssTag("insight")
    )(
      main(cls := "box box-pad page-small")(
        h1(cls := "text", dataIcon := "7")(u.username, " chess insights"),
        p(userLink(u), " has no chess insights yet!"),
        refreshForm(u, s"Generate ${u.username}'s chess insights")
      )
    )

  def forbidden(u: User)(implicit ctx: Context) =
    views.html.site.message(
      title = s"${u.username}'s chess insights are protected",
      back = routes.User.show(u.id).url.some
    )(
      p("Sorry, you cannot see ", userLink(u), "'s chess insights."),
      br,
      p(
        "Maybe ask them to change their ",
        a(cls := "button", href := routes.Pref.form("privacy"))("privacy settings"),
        " ?"
      )
    )

  def refreshForm(u: User, action: String) =
    postForm(cls := "insight-refresh", st.action := routes.Insight.refresh(u.username))(
      button(dataIcon := "E", cls := "button text")(action),
      div(cls := "crunching none")(
        spinner,
        br,
        p(strong("Now crunching data just for you!"))
      )
    )
}
