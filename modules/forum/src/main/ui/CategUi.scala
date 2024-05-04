package lila.forum
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator

final class CategUi(helpers: Helpers, bits: ForumBits):
  import helpers.{ *, given }

  def index(categs: List[CategView])(using Context) =
    Page(trans.site.forum.txt())
      .cssTag("forum")
      .csp(_.withInlineIconFont)
      .graph(
        title = "Lichess community forum",
        url = s"$netBaseUrl${routes.ForumCateg.index.url}",
        description = "Chess discussions and feedback about Lichess development"
      ):
        main(cls := "forum index box")(
          boxTop(
            h1(dataIcon := Icon.BubbleConvo, cls := "text")("Lichess Forum"),
            bits.searchForm()
          ),
          showCategs(categs.filterNot(_.categ.isTeam)),
          categs
            .exists(_.categ.isTeam)
            .option(
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

    val newTopicButton = canWrite.option(
      a(
        href     := routes.ForumTopic.form(categ.slug),
        cls      := "button button-empty button-green text",
        dataIcon := Icon.Pencil
      ):
        trans.site.createANewTopic()
    )

    def showTopic(sticky: Boolean)(topic: TopicView) =
      tr(cls := List("sticky" -> sticky))(
        td(cls := "subject")(
          a(href := routes.ForumTopic.show(categ.slug, topic.slug))(topic.name)
        ),
        td(cls := "right")(topic.nbReplies.localize),
        td(
          topic.lastPost.map: post =>
            frag(
              a(href := s"${routes.ForumTopic.show(categ.slug, topic.slug, topic.lastPage)}#${post.number}")(
                momentFromNow(post.createdAt)
              ),
              br,
              trans.site.by(bits.authorLink(post))
            )
        )
      )

    Page(categ.name)
      .cssTag("forum")
      .csp(_.withInlineIconFont)
      .js(infiniteScrollEsmInit)
      .graph(
        title = s"Forum: ${categ.name}",
        url = s"$netBaseUrl${routes.ForumCateg.show(categ.slug).url}",
        description = categ.desc
      ):
        main(cls := "forum forum-categ box")(
          boxTop(
            h1(
              a(
                href     := categ.team.fold(routes.ForumCateg.index)(routes.Team.show(_)),
                dataIcon := Icon.LessThan,
                cls      := "text"
              ),
              categ.team.fold(frag(categ.name))(teamLink(_, true))
            ),
            div(cls := "box__top__actions"):
              newTopicButton
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
              pagerNextTable(topics, n => routes.ForumCateg.show(categ.slug, n).url)
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
        val isMod = Granter.opt(_.ModerateForum)
        categs.map: view =>
          view.lastPost.map: (topic, post, page) =>
            val canBrowse = isMod || !view.categ.hidden
            val postUrl   = s"${routes.ForumTopic.show(view.slug, topic.slug, page)}#${post.number}"
            val categUrl =
              if canBrowse then routes.ForumCateg.show(view.slug).url
              else routes.ForumTopic.show(view.slug, topic.slug, 1).url
            tr(
              td(cls := "subject")(h2(a(href := categUrl)(view.name)), p(view.desc)),
              td(cls := "right")((if canBrowse then view.nbTopics else 1).localize),
              td(cls := "right")((if canBrowse then view.nbPosts else topic.nbPosts).localize),
              td(a(href := postUrl)(momentFromNow(post.createdAt)), br, trans.site.by(bits.authorLink(post)))
            )
    )
