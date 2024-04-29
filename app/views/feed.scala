package views

import lila.app.templating.Environment.{ *, given }

lazy val feed =
  lila.feed.ui
    .FeedUi(helpers, atomUi)(title => _ ?=> views.site.page.SitePage(title, "news"))(using env.executor)
