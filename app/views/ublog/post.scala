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
      moreJs = jsModule("expandText"),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${post.title}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = imageUrlOf(post),
          title = post.title,
          url = s"$netBaseUrl${routes.Ublog.post(user.username, post.slug, post.id.value)}",
          description = post.intro
        )
        .some
    ) {
      main(cls := "box box-pad page page-small ublog-post")(
        header(cls := "ublog-post__header")(
          a(href := routes.Ublog.index(user.username))(trans.ublog.xBlog(user.username))
        ),
        ctx.is(user) option standardFlash(),
        h1(cls := "ublog-post__title")(post.title),
        div(cls := "ublog-post__meta")(
          span(cls := "ublog-post__meta__author")(trans.by(userLink(user))),
          post.liveAt map { date =>
            span(cls := "ublog-post__meta__date")(semanticDate(date))
          },
          if (ctx.is(user))
            frag(
              (if (post.live) goodTag else badTag)(cls := "ublog-post__meta__publish")(
                if (post.live) trans.ublog.thisPostIsPublished() else trans.ublog.thisIsADraft()
              ),
              a(
                href := editUrlOf(post),
                cls := "button button-empty text",
                dataIcon := ""
              )(trans.edit())
            )
          else
            a(
              titleOrText(trans.reportXToModerators.txt(user.username)),
              cls := "mod report button button-red button-empty",
              href := s"${routes.Report.form}?username=${user.username}&postUrl=${urlencode(s"${netBaseUrl}${urlOf(post).url}")}&reason=comm",
              dataIcon := ""
            )
        ),
        div(cls := "ublog-post__image-wrap")(
          imageOf(post)(cls := "ublog-post__image")
        ),
        strong(cls := "ublog-post__intro")(post.intro),
        div(cls := "ublog-post__markup expand-text")(markup),
        div(cls := "ublog-post__footer")(
          a(href := routes.Ublog.index(user.username))(trans.ublog.moreBlogPostsBy(user.username))
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
        baseImg(src := picfitUrl.thumbnail(image, w, h))
      case _ =>
        div(cls := "ublog-post-image-default")
    }
  }

  private val defaultImageHeight = 500

  def imageOf(post: UblogPost, height: Int = defaultImageHeight) =
    imageUrlOf(post) match {
      case Some(url) => baseImg(src := url)
      case _ =>
        baseImg(
          heightA := height,
          src := assetUrl("images/placeholder.png")
        )
    }

  def imageUrlOf(post: UblogPost, height: Int = defaultImageHeight) = post.image map { i =>
    picfitUrl.resize(i, Right(height))
  }

  private val baseImg = img(cls := "ublog-post-image")
}
