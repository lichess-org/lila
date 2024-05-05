package views.coach

import play.api.data.Form

import lila.app.UiEnv.{ *, given }

lazy val ui = lila.coach.ui.CoachUi(helpers)(
  picfitUrl,
  lila.user.Profile.flagInfo,
  flagApi,
  netConfig.email,
  langCodes =>
    ctx ?=> lila.i18n.LangPicker.sortFor(langList.popularNoRegion.filter(l => langCodes(l.code)), ctx.req)
)

lazy val editUi = lila.coach.ui.CoachEditUi(helpers, ui)

def show(
    c: lila.coach.Coach.WithUser,
    studies: Seq[lila.study.Study.WithChaptersAndLiked],
    posts: Seq[lila.ublog.UblogPost.PreviewPost]
)(using ctx: Context) = ui.show(
  c,
  studies = studies.map(s => st.article(cls := "study")(views.study.bits.widget(s, h3))),
  posts = posts.map(views.ublog.ui.card(_))
)

def edit(c: lila.coach.Coach.WithUser, form: Form[?])(using ctx: Context) =
  editUi(c, form, views.account.ui.AccountPage(s"${c.user.titleUsername} coach page", "coach"))
