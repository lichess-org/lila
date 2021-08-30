package views.html.ublog

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogForm.UblogPostData
import lila.ublog.UblogPost
import lila.user.User

object form {

  import views.html.ublog.{ post => postView }

  def create(user: User, f: Form[UblogPostData])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog • New post"
    ) {
      main(cls := "box box-pad page ublog-post-form")(
        h1("Write a new blog post"),
        inner(user, f, none)
      )
    }

  def edit(user: User, post: UblogPost, f: Form[UblogPostData])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog • ${post.title}"
    ) {
      main(cls := "box box-pad page ublog-post-form")(
        h1("Edit your blog post"),
        div(cls := "ublog-post-form__publish")(
          p(if (post.live) "This post is published" else "This is a draft")
        ),
        imageForm(user, post),
        inner(user, f, post.some)
      )
    }

  private def imageForm(user: User, post: UblogPost)(implicit ctx: Context) =
    postForm(
      cls := "ublog-post-form__image",
      action := routes.Ublog.image(user.username, post.id.value),
      enctype := "multipart/form-data"
    )(
      form3.split(
        div(cls := "form-group form-half")(
          postView.imageOf(post, height = 300)
        ),
        div(cls := "form-group form-half")(
          p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB.")),
          form3.file.image("image"),
          submitButton(cls := "button")(trans.streamer.uploadPicture())
        )
      )
    )

  private def inner(user: User, form: Form[UblogPostData], post: Option[UblogPost])(implicit
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
      form3.checkbox(
        form("live"),
        raw("Publish this post on your blog")
      ),
      form3.actions(
        a(href := post.fold(routes.Ublog.index(user.username))(views.html.ublog.post.urlOf))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )
}
