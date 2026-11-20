package lila.insight
package ui

import play.api.libs.json.{ JsObject, Json }

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.i18n.Translate
import lila.common.Json.given

final class InsightUi(helpers: Helpers):
  import helpers.*

  def index(
      u: User,
      insightUser: InsightUser,
      prefId: Int,
      ui: JsObject,
      question: JsObject,
      stale: Boolean
  )(using me: Option[Me])(using Translate) =
    Page(trans.insight.xChessInsights.txt(u.username))
      .js(Esm("insight.refresh"))
      .js(
        PageModule(
          "insight",
          Json.obj(
            "ui" -> ui,
            "initialQuestion" -> question,
            "myUserId" -> me.map(_.userId),
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

  def empty(u: User)(using Translate) =
    Page(trans.insight.xChessInsights.txt(u.username))
      .js(Esm("insight.refresh"))
      .css("insight"):
        main(cls := "box box-pad page-small")(
          boxTop(h1(cls := "text", dataIcon := Icon.Target)(trans.insight.xChessInsights(u.username))),
          p(trans.insight.xHasNoChessInsights(userLink(u))),
          refreshForm(u, trans.insight.generateInsights.txt(u.username))
        )

  def forbidden(u: User, siteMessage: SiteMessage)(using Translate) =
    siteMessage(
      title = trans.insight.insightsAreProtected.txt(u.username),
      back = routes.User.show(u.id).url.some,
      icon = none
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

  def noGames(u: User, siteMessage: SiteMessage)(using Translate) =
    siteMessage(
      title = s"${u.username} has not played a rated game yet!",
      back = routes.User.show(u.id).url.some,
      icon = none
    ):
      p(
        "Before using chess insights,",
        userLink(u),
        " has to play at least one rated game."
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
