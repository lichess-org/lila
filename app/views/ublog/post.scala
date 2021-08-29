package views.html.ublog

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogForm
import lila.ublog.UblogPost
import lila.user.User

object post {

  def apply(user: User, post: UblogPost, markup: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog • ${post.title}"
    ) {
      main(cls := "box box-pad page ublog-post")(
        h1(cls := "ublog-post__title")(post.title),
        strong(cls := "ublog-post__intro")(post.intro),
        div(cls := "ublog-post__markup")(markup)
      )
    }

  def form(user: User, f: Form[UblogForm.UblogPostData])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog • New post"
    ) {
      main(cls := "box box-pad page ublog-post-form")(
        h1("Write a new blog post"),
        innerForm(user, f, none)
      )
    }

  private def innerForm(user: User, form: Form[UblogForm.UblogPostData], post: Option[UblogPost])(implicit
      ctx: Context
  ) =
    postForm(
      cls := "form3",
      action := post.fold(routes.Ublog.create(user.username))(p =>
        routes.Ublog.update(user.username, p.id.value)
      )
    )(
      form3.globalError(form),
      form3.group(form("title"), "Post title")(form3.input(_)(autofocus)),
      form3.group(form("intro"), "Post intro")(form3.input(_)(autofocus)),
      form3.group(
        form("markdown"),
        "Post body",
        help = markdownAvailable.some
      )(form3.textarea(_)(rows := 30)),
      form3.actions(
        a(href := post.fold(routes.Ublog.index(user.username))(bits.url))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
