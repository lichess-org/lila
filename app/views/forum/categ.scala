package views.html
package forum

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

import controllers.routes

object categ:

  def index(categs: List[lila.forum.CategView])(using PageContext) =
    views.html.base.layout(
      title = trans.forum.txt(),
      moreCss = cssTag("forum"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Lichess community forum",
          url = s"$netBaseUrl${routes.ForumCateg.index.url}",
          description = "Chess discussions and feedback about Lichess development"
        )
        .some
    ) {
      main(cls := "forum index box")(
        boxTop(
          h1(dataIcon := licon.BubbleConvo, cls := "text")("Lichess Forum"),
          bits.searchForm()
        ),
        showCategs(categs.filterNot(_.categ.isTeam)),
        categs.exists(_.categ.isTeam) option frag(
          boxTop(
            h1("Your Team Boards")
          ),
          showCategs(categs.filter(_.categ.isTeam))
        )
      )
    }

  def show(
      categ: lila.forum.ForumCateg,
      topics: Paginator[lila.forum.TopicView],
      canWrite: Boolean,
      stickyPosts: List[lila.forum.TopicView]
  )(using PageContext) =

    val newTopicButton = canWrite option
      a(
        href     := routes.ForumTopic.form(categ.slug),
        cls      := "button button-empty button-green text",
        dataIcon := licon.Pencil
      )(
        trans.createANewTopic()
      )
    def showTopic(sticky: Boolean)(topic: lila.forum.TopicView) =
      tr(cls := List("sticky" -> sticky))(
        td(cls := "subject")(
          a(href := routes.ForumTopic.show(categ.slug, topic.slug))(topic.name)
        ),
        td(cls := "right")(topic.nbReplies.localize),
        td(
          topic.lastPost.map { post =>
            frag(
              a(href := s"${routes.ForumTopic.show(categ.slug, topic.slug, topic.lastPage)}#${post.number}")(
                momentFromNow(post.createdAt)
              ),
              br,
              trans.by(bits.authorLink(post))
            )
          }
        )
      )

    views.html.base.layout(
      title = categ.name,
      moreCss = cssTag("forum"),
      moreJs = infiniteScrollTag,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"Forum: ${categ.name}",
          url = s"$netBaseUrl${routes.ForumCateg.show(categ.slug).url}",
          description = categ.desc
        )
        .some
    ) {
      main(cls := "forum forum-categ box")(
        boxTop(
          h1(
            a(
              href     := categ.team.fold(routes.ForumCateg.index)(routes.Team.show(_)),
              dataIcon := licon.LessThan,
              cls      := "text"
            ),
            categ.team.fold(frag(categ.name))(teamIdToName)
          ),
          div(cls := "box__top__actions")(
            newTopicButton
          )
        ),
        table(cls := "topics slist slist-pad")(
          thead(
            tr(
              th,
              th(cls := "right")(trans.replies()),
              th(trans.lastPost())
            )
          ),
          tbody(
            cls := "infinite-scroll",
            stickyPosts map showTopic(sticky = true),
            topics.currentPageResults map showTopic(sticky = false),
            pagerNextTable(topics, n => routes.ForumCateg.show(categ.slug, n).url)
          )
        )
      )
    }

  private def showCategs(categs: List[lila.forum.CategView])(using PageContext) =
    table(cls := "categs slist slist-pad")(
      thead(
        tr(
          th,
          th(cls := "right")(trans.topics()),
          th(cls := "right")(trans.posts()),
          th(trans.lastPost())
        )
      ),
      tbody(
        categs.map { categ =>
          tr(
            td(cls := "subject")(
              h2(a(href := routes.ForumCateg.show(categ.slug))(categ.name)),
              p(categ.desc)
            ),
            td(cls := "right")(categ.nbTopics.localize),
            td(cls := "right")(categ.nbPosts.localize),
            td(
              categ.lastPost.map { case (topic, post, page) =>
                frag(
                  a(href := s"${routes.ForumTopic.show(categ.slug, topic.slug, page)}#${post.number}")(
                    momentFromNow(post.createdAt)
                  ),
                  br,
                  trans.by(bits.authorLink(post))
                )
              }
            )
          )
        }
      )
    )
