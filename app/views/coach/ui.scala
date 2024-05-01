package views.coach

import lila.app.templating.Environment.{ *, given }
import lila.i18n.LangList

lazy val ui = lila.coach.ui.CoachUi(helpers)(
  picfitUrl,
  lila.user.Profile.flagInfo,
  flagApi,
  netConfig.email,
  langCodes =>
    ctx ?=> lila.i18n.LangPicker.sortFor(LangList.popularNoRegion.filter(l => langCodes(l.code)), ctx.req)
)

def show(
    c: lila.coach.Coach.WithUser,
    studies: Seq[lila.study.Study.WithChaptersAndLiked],
    posts: Seq[lila.ublog.UblogPost.PreviewPost]
)(using ctx: Context) = ui.show(
  c,
  studies = studies.map(s => st.article(cls := "study")(views.study.bits.widget(s, h3))),
  posts = posts.map(views.ublog.ui.card(_))
)
