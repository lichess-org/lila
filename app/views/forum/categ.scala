package views.html
package forum

import controllers.team.routes.{ Team as teamRoutes }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.forum.{ ForumTopic, ForumCateg, CategView, TopicView }

import controllers.routes

object categ:

  def index(categs: List[CategView])(using PageContext) =
    views.html.base.layout(
      title = trans.forum.txt(),
      moreCss = cssTag("forum"),
      csp = defaultCsp.withInlineIconFont.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Lichess community forum",
          url = s"$netBaseUrl${routes.ForumCateg.index.url}",
          description = "Chess discussions and feedback about Lichess development"
        )
        .some
    ):
      main(cls := "forum index box")(
        boxTop(
          h1(dataIcon := licon.BubbleConvo, cls := "text")("Lichess Forum"),
          bits.searchForm()
        ),
        showCategs(categs.filterNot(_.categ.isTeam)),
        categs.exists(_.categ.isTeam) option frag(
          boxTop(h1("Your Team Boards")),
          showCategs(categs.filter(_.categ.isTeam))
        )
      )

  def show(
      categ: ForumCateg,
      topics: Paginator[TopicView],
      canWrite: Boolean,
      stickyPosts: List[TopicView]
  )(using PageContext) =

    val newTopicButton = canWrite option
      a(
        href     := routes.ForumTopic.form(categ.slug),
        cls      := "button button-empty button-green text",
        dataIcon := licon.Pencil
      ):
        trans.createANewTopic()

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
              trans.by(bits.authorLink(post))
            )
        )
      )

    views.html.base.layout(
      title = categ.name,
      moreCss = cssTag("forum"),
      moreJs = infiniteScrollTag,
      csp = defaultCsp.withInlineIconFont.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"Forum: ${categ.name}",
          url = s"$netBaseUrl${routes.ForumCateg.show(categ.slug).url}",
          description = categ.desc
        )
        .some
    ):
      main(cls := "forum forum-categ box")(
        boxTop(
          h1(
            a(
              href     := categ.team.fold(routes.ForumCateg.index)(teamRoutes.show(_)),
              dataIcon := licon.LessThan,
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
              th(cls := "right")(trans.replies()),
              th(trans.lastPost())
            )
          ),
          tbody(cls := "infinite-scroll")(
            stickyPosts map showTopic(sticky = true),
            topics.currentPageResults map showTopic(sticky = false),
            pagerNextTable(topics, n => routes.ForumCateg.show(categ.slug, n).url)
          )
        )
      )

  private def showCategs(categs: List[CategView])(using PageContext) =
    table(cls := "categs slist slist-pad")(
      thead(
        tr(
          th,
          th(cls := "right")(trans.topics()),
          th(cls := "right")(trans.posts()),
          th(trans.lastPost())
        )
      ),
      tbody:
        categs.map: view =>
          view.lastPost.map: (topic, post, page) =>
            val canBrowse = isGranted(_.ModerateForum) || !view.categ.hidden
            val postLink  = s"${routes.ForumTopic.show(view.slug, topic.slug, page)}#${post.number}"
            val categLink = if !canBrowse then postLink else s"${routes.ForumCateg.show(view.slug)}"
            tr(
              td(cls := "subject")(h2(a(href := categLink)(view.name)), p(view.desc)),
              td(cls := "right")((if canBrowse then view.nbTopics else 1).localize),
              td(cls := "right")((if canBrowse then view.nbPosts else topic.nbPosts).localize),
              td(
                frag(a(href := postLink)(momentFromNow(post.createdAt)), br, trans.by(bits.authorLink(post)))
              )
            )
    )
