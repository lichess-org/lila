package views.html

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.user.User

import controllers.routes

object insight {

  def index(
    u: User,
    cache: lidraughts.insight.UserCache,
    prefId: Int,
    ui: play.api.libs.json.JsObject,
    question: play.api.libs.json.JsObject,
    stale: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s draughts insights",
      moreJs = frag(
        highchartsLatestTag,
        jsAt("vendor/multiple-select/multiple-select.js"),
        jsAt(s"compiled/lidraughts.insight${isProd ?? (".min")}.js"),
        jsTag("insight-refresh.js"),
        jsTag("insight-tour.js"),
        embedJsUnsafe(s"""
$$(function() {
lidraughts = lidraughts || {};
lidraughts.insight = LidraughtsInsight(document.getElementById('insight'), ${
          safeJsonValue(Json.obj(
            "ui" -> ui,
            "initialQuestion" -> question,
            "i18n" -> Json.obj(),
            "myUserId" -> ctx.userId,
            "user" -> Json.obj(
              "id" -> u.id,
              "name" -> u.username,
              "nbGames" -> cache.count,
              "stale" -> stale,
              "shareId" -> prefId
            ),
            "pageUrl" -> routes.Lobby.home.url,
            "postUrl" -> routes.Lobby.home.url
          ))
        });
});""")
      ),
      moreCss = cssTag("insight")
    )(frag(
        main(id := "insight"),
        stale option div(cls := "insight-stale none")(
          p("There are new games to learn from!"),
          refreshForm(u, "Update insights")
        )
      ))

  def empty(u: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s draughts insights",
      moreJs = jsTag("insight-refresh.js"),
      moreCss = cssTag("insight")
    )(
        main(cls := "box box-pad page-small")(
          h1(cls := "text", dataIcon := "7")(u.username, " draughts insights"),
          p(userLink(u), " has no draughts insights yet!"),
          refreshForm(u, s"Generate ${u.username}'s draughts insights")
        )
      )

  def forbidden(u: User)(implicit ctx: Context) =
    views.html.site.message(
      title = s"${u.username}'s draughts insights are protected",
      back = true,
      icon = "7".some
    )(
      p("Sorry, you cannot see ", userLink(u), "'s draughts insights."),
      br,
      p(
        "Maybe ask them to change their ",
        a(cls := "button", href := routes.Pref.form("privacy"))("privacy settings"),
        " ?"
      )
    )

  def refreshForm(u: User, action: String)(implicit ctx: Context) = emptyFrag
  /*postForm(cls := "insight-refresh", st.action := routes.Insight.refresh(u.username))(
      button(dataIcon := "E", cls := "button text")(action),
      div(cls := "crunching none")(
        spinner,
        br,
        p(strong("Now crunching data just for you!"))
      )
    )*/
}
