package views.activity

import lila.app.UiEnv.{ *, given }
import lila.core.perf.UserWithPerfs

private lazy val ui = lila.activity.ui.ActivityUi(helpers)(views.tournament.ui.tournamentIdToName)

def apply(u: UserWithPerfs, as: Iterable[lila.activity.ActivityView])(using ctx: Context) =
  ui(u, as): activity =>
    ctx.kid.no.option:
      activity.ublogPosts.map: posts =>
        ui.entryTag(
          iconTag(Icon.InkQuill),
          div(
            trans.ublog.publishedNbBlogPosts.pluralSame(posts.size),
            ui.subTag(posts.map: post =>
              div(
                a(href := routes.Ublog.post(u.user.username, post.slug, post.id))(shorten(post.title, 120))
              ))
          )
        )
