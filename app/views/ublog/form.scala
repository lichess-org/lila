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
      moreCss = cssTag("ublog"),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${trans.ublog.newPost()}"
    ) {
      main(cls := "box box-pad page page-small ublog-post-form")(
        h1(trans.ublog.newPost()),
        etiquette,
        inner(user, f, none)
      )
    }

  def edit(user: User, post: UblogPost, f: Form[UblogPostData])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = jsModule("ublog"),
      title = s"${trans.ublog.xBlog.txt(user.username)} blog • ${post.title}"
    ) {
      main(cls := "box box-pad page page-small ublog-post-form")(
        h1(trans.ublog.editYourBlogPost()),
        imageForm(user, post),
        inner(user, f, post.some),
        postForm(
          cls := "ublog-post-form__delete",
          action := routes.Ublog.delete(user.username, post.id.value)
        )(
          form3.action(
            submitButton(
              cls := "button button-red button-empty confirm",
              title := "Delete this blog post definitively"
            )(trans.delete())
          )
        )
      )
    }

  private def imageForm(user: User, post: UblogPost)(implicit ctx: Context) =
    postForm(
      cls := "ublog-post-form__image",
      action := routes.Ublog.image(post.id.value),
      enctype := "multipart/form-data"
    )(
      form3.split(
        div(cls := "form-group form-half")(formImage(post)),
        div(cls := "form-group form-half")(
          p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB.")),
          form3.file.image("image")
        )
      )
    )

  def formImage(post: UblogPost) = postView.thumbnail(post, _.Small)

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
      post.isDefined option form3.checkbox(
        form("live"),
        trans.ublog.publishOnYourBlog(),
        help = trans.ublog.publishHelp().some
      ),
      form3.group(form("title"), trans.ublog.postTitle())(form3.input(_)(autofocus)),
      form3.group(form("intro"), trans.ublog.postIntro())(form3.input(_)(autofocus)),
      form3.group(
        form("markdown"),
        trans.ublog.postBody(),
        help = frag(markdownAvailable, br, trans.embedsAvailable()).some
      )(form3.textarea(_)(rows := 30)),
      form3.actions(
        a(href := post.fold(routes.Ublog.index(user.username))(views.html.ublog.post.urlOf))(trans.cancel()),
        form3.submit(trans.apply())
      )
    )

  private def etiquette(implicit ctx: Context) = div(cls := "ublog-post-form__etiquette")(
    p("Please only post safe and respectful content."),
    p("Anything even slightly inappropriate could get your account closed."),
    a(
      dataIcon := "",
      href := routes.Page.loneBookmark("blog-etiquette"),
      cls := "text",
      targetBlank
    )("Blog Etiquette")
  )
}
