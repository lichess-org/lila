package views.appeal

import lila.app.templating.Environment.{ *, given }

lazy val tree = lila.appeal.ui.AppealTreeUi(helpers)(preset =>
  _ ?=>
    discussion.renderForm(
      lila.appeal.Appeal.form.fill(preset),
      action = routes.Appeal.post.url,
      isNew = true,
      presets = none
    )
)

def treePage(me: User, playban: Boolean, ublogIsVisible: Boolean)(using ctx: PageContext) =
  bits.layout("Appeal a moderation decision")(
    tree.page(me, playban, ublogIsVisible)
  )
