package views
package html.forum

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.forum.{ Post, RecentTopic }
import lila.security.Granter
import controllers.routes

object post {

  def recent(topics: List[RecentTopic])(implicit ctx: Context) =
    table(cls := "forum")(
      topics map { t =>
        tr(
          td(
            cls := "topic-cell",
            a(
              cls := "text",
              dataIcon := t.isTeam.option(""),
              href := routes.ForumPost.redirect(t.lastPost.postId),
              title := t.topicName,
              t.topicName
            )
          ),
          td(
            cls := "post-cell",
            if (t.allUsers.size > 1)
              span(
                span(
                  cls := "extract",
                  t.numPosts.toString + " " + trans.latestForumPostsNewBy().render + " "
                ),
                t.allUsers.toList.filter(_ != t.lastPost.userId.get) map { u =>
                  span(cls := "name", userIdLink(Option(u), withOnline = false), ", ")
                }
              ),
            span(cls := "name", userIdLink(t.lastPost.userId, withOnline = false)),
            span(cls := "text", " " + trans.latestForumPostsSaid().render + " "),
            span(cls := "extract", t.lastPost.text)
          )
        )
      }
    )

  def show(
      categ: lila.forum.Categ,
      topic: lila.forum.Topic,
      post: lila.forum.Post,
      url: String,
      canReply: Boolean,
      canModCateg: Boolean,
      canReact: Boolean
  )(implicit ctx: Context) = {
    st.article(cls := List("forum-post" -> true, "erased" -> post.erased), id := post.number)(
      div(cls := "forum-post__metas")(
        (!post.erased || canModCateg) option div(
          bits.authorLink(
            post = post,
            cssClass = s"author${(topic.userId == post.userId) ?? " author--op"}".some
          ),
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
            button(cls := "mod edit button button-empty text", tpe := "button", dataIcon := "")("Edit"),
          ctx.me flatMap { me =>
            if (!post.erased && post.canBeEditedBy(me))
              postForm(action := routes.ForumPost.delete(categ.slug, post.id))(
                submitButton(
                  cls := "mod delete button button-empty confirm",
                  dataIcon := "",
                  title := "Delete"
                )
              ).some
            else if (canModCateg)
              a(
                cls := "mod delete button button-empty",
                href := routes.ForumPost.delete(categ.slug, post.id),
                dataIcon := "",
                title := "Delete"
              ).some
            else
              post.userId map { userId =>
                val postUrl = s"${netBaseUrl}${routes.ForumPost.redirect(post.id)}"
                frag(
                  nbsp,
                  a(
                    titleOrText(trans.reportXToModerators.txt(userId)),
                    cls := "mod report button button-empty",
                    href := s"${routes.Report.form}?username=${userId}&postUrl=${urlencode(postUrl)}&reason=comm",
                    dataIcon := ""
                  )
                )
              }
          },
          (canReply && !post.erased) option button(
            cls := "mod quote button button-empty text",
            tpe := "button",
            dataIcon := "❝"
          )("Quote")
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
    val canActuallyReact = canReact && ctx.me.exists(me => !me.isBot && !post.isBy(me))
    div(cls := List("reactions" -> true, "reactions-auth" -> canActuallyReact))(
      Post.Reaction.list.map { r =>
        val users = ~post.reactions.flatMap(_ get r)
        val size  = users.size
        button(
          dataHref := canActuallyReact option routes.ForumPost.react(post.categId, post.id, r, !mine(r)).url,
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
