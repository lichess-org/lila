package views.html.ublog

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogForm.UblogPostData
import lila.ublog.UblogPost
import lila.user.User

object post {

  def apply(user: User, post: UblogPost, markup: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog • ${post.title}"
    ) {
      main(cls := "box box-pad page page-small ublog-post")(
        header(cls := "ublog-post__header")(
          a(href := routes.Ublog.index(user.username))(
            s"${user.username}' Blog"
          )
        ),
        ctx.is(user) option standardFlash(),
        h1(cls := "ublog-post__title")(post.title),
        div(cls := "ublog-post__meta")(
          span(cls := "ublog-post__meta__author")("by ", userLink(user)),
          post.liveAt map { date =>
            span(cls := "ublog-post__meta__date")(semanticDate(date))
          },
          ctx.is(user) option frag(
            (if (post.live) goodTag else badTag)(cls := "ublog-post__meta__publish")(
              if (post.live) "This post is published" else "This is a draft"
            ),
            a(
              href := editUrlOf(post),
              cls := "button button-empty text",
              dataIcon := ""
            )("Edit")
          )
        ),
        div(cls := "ublog-post__image-wrap")(
          imageOf(post, 500)(cls := "ublog-post__image")
        ),
        strong(cls := "ublog-post__intro")(post.intro),
        div(cls := "ublog-post__markup")(markup),
        div(cls := "ublog-post__footer")(
          a(href := routes.Ublog.index(user.username))("View more blog posts by ", user.username)
        )
      )
    }

  def card(post: UblogPost, makeUrl: UblogPost => Call = urlOf)(implicit ctx: Context) =
    a(cls := "ublog-post-card", href := makeUrl(post))(
      thumbnailOf(post)(cls := "ublog-post-card__image"),
      span(cls := "ublog-post-card__content")(
        h2(cls := "ublog-post-card__title")(post.title),
        span(cls := "ublog-post-card__intro")(post.intro),
        post.liveAt map semanticDate
      )
    )

  def urlOf(post: UblogPost) = routes.Ublog.post(usernameOrId(post.user), post.slug, post.id.value)

  def editUrlOf(post: UblogPost) = routes.Ublog.edit(usernameOrId(post.user), post.id.value)

  def thumbnailOf(post: UblogPost) = {
    val (w, h) = (600, 300)
    post.image match {
      case Some(image) =>
        baseImg(src := picfitUrl(image).thumbnail(w, h))
      case _ =>
        div(cls := "ublog-post-image-default")
    }
  }

  def imageOf(post: UblogPost, height: Int) =
    post.image match {
      case Some(image) =>
        baseImg(
          src := picfitUrl(image).resize(Right(height))
        )
      case _ =>
        baseImg(
          heightA := height,
          src := assetUrl("images/placeholder.png")
        )
    }

  private val baseImg = img(cls := "ublog-post-image")
}
