package lila.forum
package ui

import scalalib.paginator.Paginator

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class PostUi(helpers: Helpers, bits: ForumBits):
  import helpers.{ *, given }

  def show(
      topic: ForumTopic,
      postWithFrag: ForumPost.WithFrag,
      url: String,
      canReply: Boolean,
      canModCateg: Boolean,
      canReact: Boolean
  )(using ctx: Context) = postWithFrag match
    case ForumPost.WithFrag(post, body, hide) =>
      val postFrag = div(cls := "forum-post__message expand-text")(
        if post.erased then "<Comment deleted by user>"
        else body
      )
      st.article(cls := List("forum-post" -> true, "erased" -> post.erased), id := post.number)(
        div(cls := "forum-post__metas")(
          (!post.erased || canModCateg).option(
            div(
              ctx.blind.option(h2(a(cls := "anchor", href := url)(s"Post ${post.number}"))),
              bits.authorLink(
                post = post,
                cssClass = s"author${(topic.userId == post.userId).so(" author--op")}".some
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
              (!post.erased && ctx.me.soUse(post.shouldShowEditForm)).option(
                button(cls := "mod edit button button-empty text", tpe := "button", dataIcon := Icon.Pencil)(
                  "Edit"
                )
              ),
              ctx.me.flatMap: me =>
                given Me = me
                val quoteButton = (canReply && !post.erased).option(
                  button(
                    cls := "mod quote button button-empty text",
                    tpe := "button",
                    dataIcon := "❝"
                  )("Quote")
                )
                if !post.erased && post.canBeEditedByMe
                then
                  frag(
                    postForm(action := routes.ForumPost.delete(post.id))(
                      submitButton(
                        cls := "mod delete button button-empty yes-no-confirm",
                        dataIcon := Icon.Trash,
                        title := "Delete"
                      )
                    ),
                    quoteButton
                  ).some
                else
                  frag(
                    (canModCateg && post.number == 1).option:
                      a(
                        cls := "mod mod-relocate button button-empty",
                        href := routes.ForumPost.relocate(post.id),
                        dataIcon := Icon.Forward,
                        title := "Relocate"
                      )
                    ,
                    if canModCateg || topic.isUblogAuthor(me) then
                      frag(
                        a(
                          cls := "mod delete button button-empty",
                          href := routes.ForumPost.delete(post.id),
                          dataIcon := Icon.Trash,
                          title := "Delete"
                        ),
                        quoteButton
                      )
                    else
                      post.userId.map: userId =>
                        val postUrl = routeUrl(routes.ForumPost.redirect(post.id))
                        span(cls := "forum-post__actions")(
                          a(
                            titleOrText(trans.site.reportXToModerators.txt(userId)),
                            cls := "mod report button button-empty",
                            href := addQueryParams(
                              routes.Report.form.url,
                              Map("username" -> userId.value, "postUrl" -> postUrl.value, "from" -> "forum")
                            ),
                            dataIcon := Icon.CautionTriangle
                          ),
                          quoteButton
                        )
                  ).some
              ,
              ctx.blind.not.option:
                a(cls := "anchor", href := url)(s"#${post.number}")
            )
          )
        ),
        if hide then
          div(cls := "forum-post__blocked")(
            postFrag,
            button(cls := "button button-empty", tpe := "button")(
              "Show blocked message"
            )
          )
        else postFrag,
        (!post.erased).option:
          frag(div(cls := "forum-post__message-source")(post.text), reactions(post, canReact))
        ,
        ctx.me.soUse[Option[Tag]]:
          post.shouldShowEditForm.option:
            postForm(cls := "edit-post-form none", action := routes.ForumPost.edit(post.id))(
              lila.ui.bits.markdownTextarea("forumPostBody".some):
                textarea(
                  bits.dataTopic := topic.id,
                  name := "changes",
                  cls := "form-control post-text-area edit-post-box",
                  required
                )
              ,
              div(cls := "edit-buttons")(
                a(
                  cls := "edit-post-cancel",
                  href := routes.ForumPost.redirect(post.id),
                  style := "margin-left:20px"
                ):
                  trans.site.cancel()
                ,
                submitButton(cls := "button")(trans.site.apply())
              )
            )
      )

  def reactions(post: ForumPost, canReact: Boolean)(using ctx: Context) =
    val mine = ctx.me.so { ForumPost.Reaction.of(~post.reactions, _) }
    val canActuallyReact = canReact && ctx.me.exists(me => !me.isBot && !post.isBy(me))
    div(cls := List("reactions" -> true, "reactions-auth" -> canActuallyReact))(
      canActuallyReact.option(
        button(cls := "reactions-toggle", tpe := "button", dataIcon := Icon.PlusButton)
      ),
      ForumPost.Reaction.list.map: r =>
        val users = ~post.reactions.flatMap(_.get(r))
        val size = users.size
        button(
          dataHref := canActuallyReact.option(
            routes.ForumPost
              .react(post.categId, post.id, r.key, !mine(r))
              .url
          ),
          cls := List("mine" -> mine(r), "yes" -> (size > 0), "no" -> (size < 1)),
          title := {
            if size > 0 then
              val who =
                if size > 10 then s"${users.take(8).mkString(", ")} and ${size - 8} others"
                else users.mkString(", ")
              s"$who reacted with $r"
            else r.key
          }
        )(
          img(src := staticAssetUrl(s"images/emoji/$r.webp"), alt := r.key),
          (size > 0).option(size)
        )
    )

  def search(text: String, pager: Paginator[PostView.WithReadPerm])(using Context) =
    val title = s"""${trans.search.search.txt()} "${text.trim}""""
    Page(title)
      .css("bits.forum")
      .js(infiniteScrollEsmInit):
        main(cls := "box search")(
          boxTop(
            h1(a(href := routes.ForumCateg.index, dataIcon := Icon.LessThan, cls := "text"), title),
            bits.searchForm(text)
          ),
          strong(cls := "nb-results box__pad")(trans.site.nbForumPosts.pluralSame(pager.nbResults)),
          table(cls := "slist slist-pad slist-invert search__results")(
            (pager.nbResults > 0).option(
              tbody(cls := "infinite-scroll")(
                pager.currentPageResults.map: viewWithRead =>
                  val view = viewWithRead.view
                  val info =
                    td(cls := "info")(
                      momentFromNow(view.post.createdAt),
                      br,
                      bits.authorLink(view.post)
                    )
                  tr(cls := "paginated stack-row")(
                    if viewWithRead.canRead then
                      frag(
                        td(
                          a(cls := "post", href := routes.ForumPost.redirect(view.post.id))(
                            view.categ.name,
                            " - ",
                            view.topic.name,
                            "#",
                            view.post.number
                          ),
                          p(shorten(Markdown(view.post.text).unlink, 200))
                        ),
                        info
                      )
                    else td(colspan := "2")("[You can't access this team forum post]")
                  )
                ,
                pagerNextTable(pager, n => routes.ForumPost.search(text, n).url)
              )
            )
          )
        )
