package views
package html.forum

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.forum.ForumPost

object post:

  def recent(posts: List[lila.forum.MiniForumPost])(using PageContext) =
    ol(
      posts.map: p =>
        li(
          a(
            dataIcon := p.isTeam.option(licon.Group),
            cls      := "post_link text",
            href     := routes.ForumPost.redirect(p.postId),
            title    := p.topicName
          )(shorten(p.topicName, 30)),
          " ",
          userIdLink(p.userId, withOnline = false),
          " ",
          span(cls := "extract")(shorten(p.text, 70))
        )
    )

  def show(
      categ: lila.forum.ForumCateg,
      topic: lila.forum.ForumTopic,
      postWithFrag: lila.forum.ForumPost.WithFrag,
      url: String,
      canReply: Boolean,
      canModCateg: Boolean,
      canReact: Boolean
  )(using ctx: PageContext) = postWithFrag match
    case ForumPost.WithFrag(post, body, hide) =>
      st.article(cls := List("forum-post" -> true, "erased" -> post.erased), id := post.number)(
        div(cls := "forum-post__metas")(
          (!post.erased || canModCateg) option div(
            bits.authorLink(
              post = post,
              cssClass = s"author${(topic.userId == post.userId) so " author--op"}".some
            ),
            a(href := url)(
              post.updatedAt
                .map: updatedAt =>
                  frag(
                    span(cls := "post-edited")("edited "),
                    momentFromNow(updatedAt)
                  )
                .getOrElse:
                  momentFromNow(post.createdAt)
            ),
            (!post.erased && ctx.me.soUse(post.shouldShowEditForm)) option
              button(cls := "mod edit button button-empty text", tpe := "button", dataIcon := licon.Pencil)(
                "Edit"
              ),
            ctx.me.flatMap: me =>
              given Me = me
              if !post.erased && post.canBeEditedByMe
              then
                postForm(action := routes.ForumPost.delete(post.id))(
                  submitButton(
                    cls      := "mod delete button button-empty confirm",
                    dataIcon := licon.Trash,
                    title    := "Delete"
                  )
                ).some
              else
                frag(
                  if canModCateg || topic.isUblogAuthor(me) then
                    a(
                      cls      := "mod delete button button-empty",
                      href     := routes.ForumPost.delete(post.id),
                      dataIcon := licon.Trash,
                      title    := "Delete"
                    )
                  else
                    post.userId.map: userId =>
                      val postUrl = s"${netBaseUrl}${routes.ForumPost.redirect(post.id)}"
                      frag(
                        nbsp,
                        a(
                          titleOrText(trans.reportXToModerators.txt(userId)),
                          cls := "mod report button button-empty",
                          href := addQueryParams(
                            reportRoutes.form.url,
                            Map("username" -> userId, "postUrl" -> postUrl, "reason" -> "comm")
                          ),
                          dataIcon := licon.CautionTriangle
                        )
                      )
                ).some
            ,
            (canReply && !post.erased) option button(
              cls      := "mod quote button button-empty text",
              tpe      := "button",
              dataIcon := "❝"
            )("Quote")
          ),
          a(cls := "anchor", href := url)(s"#${post.number}")
        ),
        frag:
          val postFrag = div(cls := s"forum-post__message expand-text")(
            if post.erased then "<Comment deleted by user>"
            else body
          )
          if hide then
            div(cls := "forum-post__blocked")(
              postFrag,
              button(cls := "button button-empty", tpe := "button")(
                "Show blocked message"
              )
            )
          else postFrag
        ,
        !post.erased option reactions(post, canReact),
        ctx.me.soUse(post.shouldShowEditForm) option
          postForm(cls := "edit-post-form", action := routes.ForumPost.edit(post.id))(
            textarea(
              bits.dataTopic := topic.id,
              name           := "changes",
              cls            := "post-text-area edit-post-box",
              minlength      := 3,
              required
            )(post.text),
            div(cls := "edit-buttons")(
              a(
                cls   := "edit-post-cancel",
                href  := routes.ForumPost.redirect(post.id),
                style := "margin-left:20px"
              ):
                trans.cancel()
              ,
              submitButton(cls := "button")(trans.apply())
            )
          )
      )

  def reactions(post: ForumPost, canReact: Boolean)(using ctx: PageContext) =
    val mine             = ctx.me so { ForumPost.Reaction.of(~post.reactions, _) }
    val canActuallyReact = canReact && ctx.me.exists(me => !me.isBot && !post.isBy(me))
    div(cls := List("reactions" -> true, "reactions-auth" -> canActuallyReact))(
      ForumPost.Reaction.list.map: r =>
        val users = ~post.reactions.flatMap(_ get r)
        val size  = users.size
        button(
          dataHref := canActuallyReact option routes.ForumPost
            .react(post.categId, post.id, r.key, !mine(r))
            .url,
          cls := List("mine" -> mine(r), "yes" -> (size > 0), "no" -> (size < 1)),
          title := {
            if size > 0 then
              val who =
                if size > 10 then s"${users take 8 mkString ", "} and ${size - 8} others"
                else users mkString ", "
              s"$who reacted with $r"
            else r.key
          }
        )(
          img(src := assetUrl(s"images/emoji/$r.png"), alt := r.key),
          size > 0 option size
        )
    )
