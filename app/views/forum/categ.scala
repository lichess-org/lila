package views.html
package forum

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator

import controllers.routes

object categ {

  def index(categs: List[lidraughts.forum.CategView])(implicit ctx: Context) = bits.layout(
    title = trans.forum.txt(),
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Lidraughts community forum",
      url = s"$netBaseUrl${routes.ForumCateg.index.url}",
      description = "Draughts discussions and feedback about lidraughts development"
    ).some
  ) {
      main(cls := "forum index box")(
        h1(dataIcon := "d", cls := "text")("Lidraughts Forum"),
        showCategs(categs.filterNot(_.categ.isTeam)),
        if (categs.exists(_.categ.isTeam)) frag(
          h1("Your teams boards"),
          showCategs(categs.filter(_.categ.isTeam))
        )
      )
    }

  private def showCategs(categs: List[lidraughts.forum.CategView])(implicit ctx: Context) =
    table(cls := "categs slist slist-pad")(
      thead(
        tr(
          th,
          th(cls := "right")(trans.topics.frag()),
          th(cls := "right")(trans.posts.frag()),
          th(trans.lastPost.frag())
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
              categ.lastPost.map {
                case (topic, post, page) => frag(
                  a(href := s"${routes.ForumTopic.show(categ.slug, topic.slug, page)}#${post.number}")(
                    momentFromNow(post.createdAt)
                  ),
                  br,
                  trans.by.frag(authorName(post))
                )
              }
            )
          )
        }
      )
    )

  def show(
    categ: lidraughts.forum.Categ,
    topics: Paginator[lidraughts.forum.TopicView],
    canWrite: Boolean,
    stickyPosts: List[lidraughts.forum.TopicView]
  )(implicit ctx: Context) = {

    val newTopicButton = canWrite option
      a(href := routes.ForumTopic.form(categ.slug), cls := "button button-empty button-green text", dataIcon := "m")(
        trans.createANewTopic.frag()
      )
    def showTopic(sticky: Boolean)(topic: lidraughts.forum.TopicView) = tr(
      td(cls := "subject")(
        a(href := routes.ForumTopic.show(categ.slug, topic.slug), cls := List("sticky" -> sticky))(
          sticky option iconTag("")(title := "Sticky"),
          topic.name
        )
      ),
      td(cls := "right")(topic.views.localize),
      td(cls := "right")(topic.nbReplies.localize),
      td(
        topic.lastPost.map { post =>
          frag(
            a(href := s"${routes.ForumTopic.show(categ.slug, topic.slug, topic.lastPage)}#${post.number}")(
              momentFromNow(post.createdAt)
            ),
            br,
            trans.by.frag(authorLink(post))
          )
        }
      )
    )
    def showBar(pos: String) =
      div(cls := s"bar $pos")(
        bits.pagination(routes.ForumCateg.show(categ.slug, 1), topics, showPost = false),
        newTopicButton
      )

    bits.layout(
      title = categ.name,
      menu = mod.menu("forum").toHtml.some.ifTrue(categ.isStaff),
      openGraph = lidraughts.app.ui.OpenGraph(
        title = s"Forum: ${categ.name}",
        url = s"$netBaseUrl${routes.ForumCateg.show(categ.slug).url}",
        description = categ.desc
      ).some
    ) {
        main(cls := "forum categ box")(
          h1(
            a(
              href := categ.team.fold(routes.ForumCateg.index)(routes.Team.show(_)),
              dataIcon := "I",
              cls := "text"
            ),
            categ.team.fold(frag(categ.name))(teamIdToName)
          ),
          p(cls := "description")(categ.desc),
          showBar("top"),
          table(cls := "topics slist slist-pad")(
            thead(
              tr(
                th,
                th(cls := "right")(trans.views.frag()),
                th(cls := "right")(trans.replies.frag()),
                th(trans.lastPost.frag())
              )
            ),
            tbody(
              stickyPosts map showTopic(true),
              topics.currentPageResults map showTopic(false)
            )
          ),
          showBar("bottom")
        )
      }
  }
}
