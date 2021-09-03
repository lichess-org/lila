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

  def apply(user: User, post: UblogPost, markup: Frag, others: List[UblogPost.PreviewPost])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      moreJs = frag(jsModule("expandText"), ctx.is(user) option jsModule("ublog")),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${post.title}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = post.image.isDefined option thumbnail.url(post, _.Large),
          title = post.title,
          url = s"$netBaseUrl${routes.Ublog.post(user.username, post.slug, post.id.value)}",
          description = post.intro
        )
        .some
    ) {
      main(cls                       := "box box-pad page page-small ublog-post")(
        thumbnail(post, _.Large)(cls := "ublog-post__image"),
        ctx.is(user) option standardFlash(),
        h1(cls := "ublog-post__title")(post.title),
        div(cls := "ublog-post__meta")(
          span(cls := "ublog-post__meta__author")(
            trans.by(
              a(
                href     := routes.Ublog.index(user.username),
                cls      := userClass(user.id, none, withOnline = true),
                dataHref := routes.User.show(user.username)
              )(lineIcon(user), titleTag(user.title), user.username)
            )
          ),
          post.liveAt map { date =>
            span(cls := "ublog-post__meta__date")(semanticDate(date))
          },
          if (ctx.is(user))
            frag(
              (if (post.live) goodTag else badTag)(cls := "ublog-post__meta__publish")(
                if (post.live) trans.ublog.thisPostIsPublished() else trans.ublog.thisIsADraft()
              ),
              a(
                href     := editUrlOf(post),
                cls      := "button button-empty text",
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
        strong(cls := "ublog-post__intro")(post.intro),
        div(cls := "ublog-post__markup expand-text")(markup),
        div(cls := "ublog-post__footer")(
          h2(a(href := routes.Ublog.index(user.username))(trans.ublog.moreBlogPostsBy(user.username))),
          others.size > 0 option div(cls := "ublog-post-cards")(others map { card(_) })
        )
      )
    }

  def card(post: UblogPost.BasePost, makeUrl: UblogPost.BasePost => Call = urlOf)(implicit ctx: Context) =
    a(cls                          := "ublog-post-card", href := makeUrl(post))(
      thumbnail(post, _.Small)(cls := "ublog-post-card__image"),
      span(cls := "ublog-post-card__content")(
        h2(cls := "ublog-post-card__title")(post.title),
        span(cls := "ublog-post-card__intro")(post.intro),
        post.liveAt map semanticDate
      )
    )

  def urlOf(post: UblogPost.BasePost) = routes.Ublog.post(usernameOrId(post.user), post.slug, post.id.value)

  def editUrlOf(post: UblogPost.BasePost) = routes.Ublog.edit(usernameOrId(post.user), post.id.value)

  object thumbnail {
    sealed abstract class Size(val width: Int) {
      def height = width * 10 / 16
    }
    case object Large extends Size(880)
    case object Small extends Size(400)

    def apply(post: UblogPost.BasePost, size: thumbnail.type => Size) =
      img(cls := "ublog-post-image")(src := url(post, size))

    def url(post: UblogPost.BasePost, size: thumbnail.type => Size) = {
      val s = size(thumbnail)
      post.image match {
        case Some(image) => picfitUrl.thumbnail(image, s.width, s.height)
        case _           => assetUrl("images/user-blog-default.png")
      }
    }
  }
}
