package views
package html.forum

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.forum.Post
import lila.security.Granter

import controllers.routes

object post {

  def recent(posts: List[lila.forum.MiniForumPost])(implicit ctx: Context) =
    ol(
      posts map { p =>
        li(
          a(
            dataIcon := p.isTeam.option(""),
            cls := "post_link text",
            href := routes.ForumPost.redirect(p.postId),
            title := p.topicName
          )(
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
      canModCateg: Boolean,
      canReact: Boolean
  )(implicit ctx: Context) = {
    st.article(cls := List("forum-post" -> true, "erased" -> post.erased), id := post.number)(
      div(cls := "forum-post__metas")(
        (!post.erased || canModCateg) option div(
          authorLink(post = post, cssClass = "author".some, modIcon = post.displayModIcon),
          a(href := url)(
            post.updatedAt
              .map { updatedAt =>
                frag(
                  span(cls := "post-edited")("edited "),
                  momentFromNow(updatedAt)
                )
              }
              .getOrElse {
                momentFromNow(post.createdAt)
              }
          ),
          (!post.erased && ctx.me.exists(post.shouldShowEditForm)) option
            a(cls := "mod edit button button-empty text", dataIcon := "")("Edit"),
          if (!post.erased && ctx.me.exists(post.canBeEditedBy))
            postForm(action := routes.ForumPost.delete(categ.slug, post.id))(
              submitButton(
                cls := "mod delete button button-empty confirm",
                dataIcon := "",
                title := "Delete"
              )
            )
          else if (canModCateg)
            a(
              cls := "mod delete button button-empty",
              href := routes.ForumPost.delete(categ.slug, post.id),
              dataIcon := "",
              title := "Delete"
            )
          else
            post.userId map { userId =>
              val postUrl = s"${netBaseUrl}${routes.ForumPost.redirect(post.id)}"
              frag(
                nbsp,
                a(
                  titleOrText(trans.reportXToModerators.txt(userId)),
                  cls := "mod report button button-empty",
                  href := s"${routes.Report.form}?username=${userId}&postUrl=${urlencode(postUrl)}",
                  dataIcon := ""
                )
              )
            }
        ),
        a(cls := "anchor", href := url)(s"#${post.number}")
      ),
      p(cls := "forum-post__message")(
        if (post.erased) "<Comment deleted by user>"
        else richText(post.text)
      ),
      !post.erased option reactions(post, canReact),
      ctx.me.exists(post.shouldShowEditForm) option
        postForm(cls := "edit-post-form", action := routes.ForumPost.edit(post.id))(
          textarea(
            bits.dataTopic := topic.id,
            name := "changes",
            cls := "post-text-area edit-post-box",
            minlength := 3,
            required
          )(post.text),
          div(cls := "edit-buttons")(
            a(
              cls := "edit-post-cancel",
              href := routes.ForumPost.redirect(post.id),
              style := "margin-left:20px"
            )(
              trans.cancel()
            ),
            submitButton(cls := "button")(trans.apply())
          )
        )
    )
  }

  def reactions(post: Post, canReact: Boolean)(implicit ctx: Context) = {
    val mine             = ctx.me ?? { Post.Reaction.of(~post.reactions, _) }
    val canActuallyReact = canReact && ctx.me.exists(!_.isBot)
    div(cls := List("reactions" -> true, "reactions-auth" -> canActuallyReact))(
      Post.Reaction.list.map { r =>
        val users = ~post.reactions.flatMap(_ get r)
        val size  = users.size
        button(
          dataHref := canActuallyReact option routes.ForumPost.react(post.id, r, !mine(r)).url,
          cls := List("mine" -> mine(r), "yes" -> (size > 0), "no" -> (size < 1)),
          title := {
            if (size > 0) {
              val who =
                if (size > 10) s"${users take 8 mkString ", "} and ${size - 8} others"
                else users mkString ", "
              s"$who reacted with $r"
            } else r
          }
        )(
          img(src := assetUrl(s"images/emoji/$r.png"), alt := r),
          size > 0 option size
        )
      }
    )
  }
}
