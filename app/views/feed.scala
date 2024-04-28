package views

import lila.app.templating.Environment.{ *, given }

lazy val feed = lila.feed.ui.FeedUi(helpers, atomUi)(views.site.page.wrap(_))(using env.executor)
