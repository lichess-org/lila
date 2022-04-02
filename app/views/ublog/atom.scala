package views.html.ublog

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.ublog.{ UblogBlog, UblogPost }
import lila.user.User

object atom {

  import views.html.base.atom.{ atomDate, category }
  import views.html.ublog.blog.urlOfBlog
  import views.html.ublog.post.{ thumbnail, urlOfPost }

  def user(
      user: User,
      blog: UblogBlog,
      posts: Seq[UblogPost.PreviewPost]
  )(implicit lang: Lang) =
    views.html.base.atom(
      elems = posts,
      htmlCall = routes.Ublog.index(user.username),
      atomCall = routes.Ublog.userAtom(user.username),
      title = trans.ublog.xBlog.txt(user.username),
      updated = posts.headOption.flatMap(_.lived).map(_.at)
    ) { post =>
      renderPost(post, authorOfBlog(post.blog))
    }

  def community(code: String, posts: Seq[UblogPost.PreviewPost]) =
    views.html.base.atom(
      elems = posts,
      htmlCall = routes.Ublog.community(code),
      atomCall = routes.Ublog.communityAtom(code),
      title = "Lichess community blogs",
      updated = posts.headOption.flatMap(_.lived).map(_.at)
    ) { post =>
      renderPost(post, authorOfBlog(post.blog))
    }

  private def renderPost(post: UblogPost.PreviewPost, authorName: String) =
    frag(
      tag("id")(post._id),
      tag("published")(post.lived.map(_.at) map atomDate),
      link(
        rel  := "alternate",
        tpe  := "text/html",
        href := s"$netBaseUrl${urlOfPost(post)}"
      ),
      tag("title")(post.title),
      post.topics.map { topic =>
        category(
          term = topic.url,
          label = topic.value,
          scheme = s"$netBaseUrl${routes.Ublog.topic(topic.url)}".some
        )
      },
      tag("content")(tpe := "html")(
        thumbnail(post, _.Large),
        "<br>", // yes, scalatags encodes it.
        post.intro
      ),
      tag("tag")("media:thumbnail")(attr("url") := thumbnail.url(post, _.Large)),
      tag("author")(tag("name")(authorName))
    )

  private def authorOfBlog(blogId: UblogBlog.Id): String = blogId match {
    case UblogBlog.Id.User(userId) => titleNameOrId(userId)
  }
}
