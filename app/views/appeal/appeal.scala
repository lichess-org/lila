package views.appeal

import lila.app.UiEnv.{ *, given }

lazy val ui = lila.appeal.ui.AppealUi(helpers)

lazy val tree = lila.appeal.ui.AppealTreeUi(helpers, ui)(topic =>
  preset =>
    _ ?=>
      discussion.renderForm(
        lila.appeal.Appeal.form.fill(preset),
        action = routes.Appeal.post(topic),
        isNew = true,
        presets = none
      )
)
