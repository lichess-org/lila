package views.html
package forum

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.forum.{ ForumPost, RecentForumTopic }
import lila.security.{ Granter, Permission }

object bits:

  def searchForm(search: String = "")(using PageContext) =
    div(cls := "box__top__actions")(
      form(cls := "search", action := routes.ForumPost.search())(
        input(
          name         := "text",
          value        := search,
          placeholder  := trans.search.search.txt(),
          enterkeyhint := "search"
        )
      )
    )

  val categs = Map(
    "general-chess-discussion"   -> "CHESS",
    "offtopic-discussion"        -> "OFFTOPIC",
    "lichess-feedback"           -> "FEEDBACK",
    "game-analysis"              -> "ANALYSIS",
    "community-blog-discussions" -> "BLOG"
  ).map(c => ForumCategId(c._1) -> c._2)

  def recentTopics(forumTopics: List[RecentForumTopic])(using PageContext) =
    forumTopics.map { topic =>
      val mostRecent = topic.posts.last
      div(
        span(
          a(
            href  := routes.ForumCateg.show(topic.categId),
            title := topic.categId.split("-").map(_.capitalize).mkString(" "),
            cls   := s"categ"
          )(
            categs.get(topic.categId).getOrElse("TEAM")
          ),
          ": ",
          a(
            href  := routes.ForumPost.redirect(mostRecent.postId),
            title := s"${topic.name}\n\n${titleNameOrAnon(mostRecent.userId)}:  ${mostRecent.text}"
          )(
            topic.name
          )
        ),
        div(cls := "contributors")(
          topic.contribs.map(userIdLink(_)).join(" · "),
          span(cls := "time", momentFromNow(topic.updatedAt))
        )
      )
    }

  def authorLink(post: ForumPost, cssClass: Option[String] = None, withOnline: Boolean = true)(using
      PageContext
  ): Frag =
    if !Granter.opt(_.ModerateForum) && post.erased
    then span(cls := "author")("<erased>")
    else userIdLink(post.userId, cssClass = cssClass, withOnline = withOnline, modIcon = ~post.modIcon)

  private[forum] val dataTopic = attr("data-topic")
  private[forum] val dataUnsub = attr("data-unsub")
