package views
package html.forum

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object post {

  def recent(posts: List[lila.forum.MiniForumPost])(implicit ctx: Context) = ol(
    posts map { p =>
      li(
        a(dataIcon := p.isTeam.option("f"), cls := "post_link text", href := routes.ForumPost.redirect(p.postId), title := p.topicName)(
          shorten(p.topicName, 30)
        ),
        " ",
        userIdLink(p.userId, withOnline = false),
        " ",
        span(cls := "extract")(shorten(p.text, 70))
      )
    }
  )

  def show(
    categ: lila.forum.Categ,
    topic: lila.forum.Topic,
    post: lila.forum.Post,
    url: String,
    canModCateg: Boolean
  )(implicit ctx: Context) = {
    st.article(cls := List("forum-post" -> true, "erased" -> post.erased), id := post.number)(
      div(cls := "forum-post__metas")(
        div(
          authorLink(post = post, cssClass = "author".some, modIcon = post.displayModIcon),
          a(href := url)(
            post.updatedAt.map { updatedAt =>
              frag(
                span(cls := "post-edited")("edited "),
                momentFromNow(updatedAt)
              )
            }.getOrElse {
              momentFromNow(post.createdAt)
            }
          ),
          isGranted(_.IpBan) option span(cls := "mod postip")(post.ip),
          ctx.userId.fold(false)(post.shouldShowEditForm(_)) option
            a(cls := "mod edit button button-empty text", dataIcon := "m")("Edit"),
          canModCateg option a(
            cls := "mod delete button button-empty button-red",
            href := routes.ForumPost.delete(categ.slug, post.id),
            dataIcon := "q",
            title := "Delete"
          )
        ),
        a(cls := "anchor", href := url)(s"#${post.number}")
      ),
      p(cls := "forum-post__message")(
        if (post.erased) "<erased>"
        else richText(post.text)
      ),
      ctx.userId.exists(post.shouldShowEditForm(_)) option
        st.form(cls := "edit-post-form", method := "post", action := routes.ForumPost.edit(post.id))(
          textarea(
            bits.dataTopic := topic.id,
            name := "changes",
            cls := "post-text-area edit-post-box",
            minlength := 3,
            required
          )(post.text),
          div(cls := "edit-buttons")(
            a(cls := "edit-post-cancel", href := routes.ForumPost.redirect(post.id), style := "margin-left:20px")(
              trans.cancel()
            ),
            button(`type` := "submit", cls := "button")(trans.apply())
          )
        )
    )
  }
}
