package views.ublog

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.i18n.LangList
import lila.core.i18n.Language
import lila.ublog.UblogPost

lazy val ui = lila.ublog.ui.UblogUi(helpers, views.atomUi)(picfitUrl)

lazy val post = lila.ublog.ui.UblogPostUi(helpers, ui)(
  ublogRank = env.ublog.rank,
  connectLinks = views.bits.connectLinks
)

lazy val form = lila.ublog.ui.UblogFormUi(helpers, ui)(
  renderCaptcha = (form, captcha) =>
    _ ?=>
      captcha.fold(views.captcha.hiddenEmpty(form)):
        views.captcha(form, _)
)

def community(language: Option[Language], posts: Paginator[UblogPost.PreviewPost])(using ctx: Context) =
  val langSelections: List[(String, String)] = ("all", "All languages") ::
    lila.i18n.LangPicker
      .sortFor(LangList.popularNoRegion, ctx.req)
      .map: l =>
        l.language -> LangList.name(l)
  ui.community(language, posts, langSelections)
