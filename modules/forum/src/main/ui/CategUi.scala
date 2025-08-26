package lila.forum
package ui

import scalalib.paginator.Paginator

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class CategUi(helpers: Helpers, bits: ForumBits):
  import helpers.{ *, given }

  def index(categs: List[CategView])(using Context) =
    Page(trans.site.forum.txt())
      .css("bits.forum")
      .csp(_.withInlineIconFont)
      .graph(
        title = "Lichess community forum",
        url = s"$netBaseUrl${routes.ForumCateg.index.url}",
        description = "Chess discussions and feedback about Lichess development"
      ):
        val (teamCategs, globalCategs) = categs.partition(_.categ.isTeam)
        main(cls := "forum index box")(
          boxTop(
            h1(dataIcon := Icon.BubbleConvo, cls := "text")("Lichess Forum"),
            bits.searchForm()
          ),
          showCategs(globalCategs),
          teamCategs.nonEmpty.option(
            frag(
              boxTop(h1("Your Team Boards")),
              showCategs(categs.filter(_.categ.isTeam))
            )
          )
        )

  def show(
      categ: lila.forum.ForumCateg,
      topics: Paginator[TopicView],
      canWrite: Boolean,
      stickyPosts: List[TopicView]
  )(using Context) =

    def showTopic(sticky: Boolean)(topic: TopicView) =
      tr(cls := List("sticky" -> sticky))(
        td(cls := "subject")(
          a(href := routes.ForumTopic.show(categ.id, topic.slug))(topic.name)
        ),
        td(cls := "right")(topic.nbReplies.localize),
        td(
          topic.lastPost.map: post =>
            frag(
              a(href := s"${routes.ForumTopic.show(categ.id, topic.slug, topic.lastPage)}#${post.number}")(
                momentFromNow(post.createdAt)
              ),
              br,
              trans.site.by(bits.authorLink(post))
            )
        )
      )

    Page(categ.name)
      .css("bits.forum")
      .csp(_.withInlineIconFont)
      .js(infiniteScrollEsmInit)
      .graph(
        title = s"Forum: ${categ.name}",
        url = s"$netBaseUrl${routes.ForumCateg.show(categ.id).url}",
        description = categ.desc
      ):
        main(cls := "forum forum-categ box")(
          boxTop(
            h1(
              a(
                href := categ.team.fold(routes.ForumCateg.index)(routes.Team.show(_)),
                dataIcon := Icon.LessThan,
                cls := "text"
              ),
              categ.team.fold(frag(categ.name))(teamLink(_, true))
            ),
            div(cls := "box__top__actions")(
              Granter
                .opt(_.ModerateForum)
                .option(
                  a(
                    href := routes.ForumCateg.modFeed(categ.id),
                    cls := "button button-empty text"
                  )("Mod feed")
                ),
              canWrite.option(
                a(
                  href := routes.ForumTopic.form(categ.id),
                  cls := "button button-empty button-green text",
                  dataIcon := Icon.Pencil
                )(trans.site.createANewTopic())
              )
            )
          ),
          table(cls := "topics slist slist-pad")(
            thead(
              tr(
                th,
                th(cls := "right")(trans.site.replies()),
                th(trans.site.lastPost())
              )
            ),
            tbody(cls := "infinite-scroll")(
              stickyPosts.map(showTopic(sticky = true)),
              topics.currentPageResults.map(showTopic(sticky = false)),
              pagerNextTable(topics, n => routes.ForumCateg.show(categ.id, n).url)
            )
          )
        )

  private def showCategs(categs: List[CategView])(using Context) =
    table(cls := "categs slist slist-pad")(
      thead(
        tr(
          th,
          th(cls := "right")(trans.site.topics()),
          th(cls := "right")(trans.site.posts()),
          th(trans.site.lastPost())
        )
      ),
      tbody:
        categs.map: view =>
          view.lastPost match
            case None =>
              tr( // the last post was deleted!
                td(cls := "subject")(
                  h2(a(href := routes.ForumCateg.show(view.slug))(view.name)),
                  p(view.desc)
                ),
                td(cls := "right")(view.nbTopics.localize),
                td(cls := "right")(view.nbPosts.localize),
                td
              )
            case Some((topic, post, page)) =>
              val canBrowse = !view.categ.hidden
                || Granter.opt(_.ModerateForum)
                || (view.categ.isDiagnostic && Granter.opt(_.Diagnostics))
              val postUrl = s"${routes.ForumTopic.show(view.slug, topic.slug, page)}#${post.number}"
              val categUrl =
                if canBrowse then routes.ForumCateg.show(view.slug)
                else routes.ForumTopic.show(view.slug, topic.slug, 1)
              tr(
                td(cls := "subject")(h2(a(href := categUrl)(view.name)), p(view.desc)),
                td(cls := "right")((if canBrowse then view.nbTopics else 1).localize),
                td(cls := "right")((if canBrowse then view.nbPosts else topic.nbPosts).localize),
                td(
                  a(href := postUrl)(momentFromNow(post.createdAt)),
                  br,
                  trans.site.by(bits.authorLink(post))
                )
              )
    )

  def modFeed(
      categ: lila.forum.ForumCateg,
      posts: Paginator[PostView]
  )(using Context) =
    paginationByQuery(routes.ForumCateg.modFeed(categ.id, 1), posts, showPost = true)
    Page(categ.name)
      .css("bits.forum")
      .csp(_.withInlineIconFont)
      .js(infiniteScrollEsmInit):
        main(cls := "forum forum-mod-feed box")(
          boxTop(
            h1(
              a(
                href := routes.ForumCateg.show(categ.id),
                dataIcon := Icon.LessThan,
                cls := "text"
              )(categ.name),
              " mod feed"
            )
          ),
          table(cls := "slist slist-pad")(
            thead(tr(th("User"), th("Topic"), th("Post"), th("Date"))),
            tbody(cls := "infinite-scroll")(
              posts.currentPageResults.map: p =>
                tr(cls := "paginated")(
                  td(userIdLink(p.post.userId)),
                  td(a(href := routes.ForumTopic.show(p.categ.id, p.topic.slug))(p.topic.name)),
                  td(shorten(p.post.text, 400)),
                  td(a(href := routes.ForumPost.redirect(p.post.id))(momentFromNow(p.post.createdAt)))
                ),
              pagerNextTable(posts, np => routes.ForumCateg.modFeed(categ.id, np).url)
            )
          )
        )
