package views.html.ublog

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.user.User
import lila.ublog.{ UblogBlog, UblogPost }

object atom {

  import views.html.base.atom.atomDate
  import views.html.ublog.blog.urlOfBlog
  import views.html.ublog.post.{ thumbnail, urlOfPost }

  def apply(
      user: User,
      blog: UblogBlog,
      posts: Seq[UblogPost.PreviewPost]
  ) =
    views.html.base.atom(
      elems = posts,
      htmlCall = routes.Ublog.index(user.username),
      atomCall = routes.Blog.atom,
      title = "lichess.org blog",
      updated = posts.headOption.flatMap(_.lived).map(_.at)
    ) { post =>
      frag(
        tag("id")(s"$netBaseUrl${urlOfBlog(blog)}"),
        tag("published")(post.lived.map(_.at) map atomDate),
        link(
          rel := "alternate",
          tpe := "text/html",
          href := s"$netBaseUrl${urlOfPost(post)}"
        ),
        tag("title")(post.title),
        // tag("category")(
        //   tag("term")(doc.getText("blog.category")),
        //   tag("label")(slugify(~doc.getText("blog.category")))
        // ),
        tag("content")(tpe := "html")(
          thumbnail(post, _.Large),
          "<br>", // yes, scalatags encodes it.
          post.intro
        ),
        tag("tag")("media:thumbnail")(attr("url") := thumbnail.url(post, _.Large)),
        tag("author")(tag("name")(user.titleUsername))
      )
    }
}
