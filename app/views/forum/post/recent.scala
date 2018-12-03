package views
package html.forum.post

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._

import controllers.routes

object recent {

  def apply(posts: List[lila.forum.MiniForumPost])(implicit ctx: Context) = ol(
    posts map { p =>
      li(
        a(dataIcon := p.isTeam.option("f"), cls := "post_link", href := routes.ForumPost.redirect(p.postId), title := p.topicName)(
          shorten(p.topicName, 30)
        ),
        " ",
        userIdLink(p.userId, withOnline = false),
        " ",
        span(cls := "extract")(shorten(p.text, 70))
      )
    }
  )
}
