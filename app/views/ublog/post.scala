package views.html.ublog

import controllers.routes
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogForm.UblogPostData
import lila.ublog.{ UblogBlog, UblogPost }
import lila.user.User

object post {

  def apply(
      user: User,
      blog: UblogBlog,
      post: UblogPost,
      markup: Frag,
      others: List[UblogPost.PreviewPost],
      liked: Boolean
  )(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      moreJs = frag(
        jsModule("expandText"),
        ctx.isAuth option jsModule("ublog")
      ),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${post.title}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = post.image.isDefined option thumbnail.url(post, _.Large),
          title = post.title,
          url = s"$netBaseUrl${routes.Ublog.post(user.username, post.slug, post.id.value)}",
          description = post.intro
        )
        .some,
      robots = netConfig.crawlable && blog.listed
    ) {
      main(cls := "box box-pad page page-small ublog-post")(
        thumbnail(post, _.Large)(cls := "ublog-post__image"),
        ctx.is(user) option standardFlash(),
        h1(cls := "ublog-post__title")(post.title),
        div(cls := "ublog-post__meta")(
          a(
            href := routes.Ublog.index(user.username),
            cls := userClass(user.id, none, withOnline = true),
            dataHref := routes.User.show(user.username)
          )(
            lineIcon(user),
            titleTag(user.title),
            user.username,
            isGranted(_.ModerateBlog) option (if (blog.tier <= UblogBlog.Tier.VISIBLE) badTag else goodTag)(
              cls := "ublog-post__tier"
            )(UblogBlog.Tier.name(blog.tier))
          ),
          post.lived map { live =>
            span(cls := "ublog-post__meta__date")(semanticDate(live.at))
          },
          button(
            tpe := "button",
            cls := List(
              "ublog-post__like button-link is" -> true,
              "ublog-post__like--liked"         -> liked
            ),
            dataRel := post.id.value,
            title := trans.study.like.txt()
          )(post.likes.value),
          span(cls := "ublog-post__views")(
            trans.ublog.nbViews.plural(post.views.value, strong(post.views.value))
          ),
          if (ctx is user)
            frag(
              (if (post.live) goodTag else badTag)(cls := "ublog-post__meta__publish")(
                if (post.live) trans.ublog.thisPostIsPublished() else trans.ublog.thisIsADraft()
              ),
              a(
                href := editUrlOfPost(post),
                cls := "button button-empty text",
                dataIcon := ""
              )(trans.edit())
            )
          else
            a(
              titleOrText(trans.reportXToModerators.txt(user.username)),
              cls := "button button-empty ublog-post__meta__report",
              href := s"${routes.Report.form}?username=${user.username}&postUrl=${urlencode(s"${netBaseUrl}${urlOfPost(post).url}")}&reason=comm",
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

  def card(
      post: UblogPost.BasePost,
      makeUrl: UblogPost.BasePost => Call = urlOfPost,
      showAuthor: Boolean = false
  )(implicit ctx: Context) =
    a(cls := "ublog-post-card", href := makeUrl(post))(
      thumbnail(post, _.Small)(cls := "ublog-post-card__image"),
      span(cls := "ublog-post-card__content")(
        h2(cls := "ublog-post-card__title")(post.title),
        span(cls := "ublog-post-card__intro")(post.intro),
        post.lived map { live => semanticDate(live.at)(ctx.lang)(cls := "ublog-post-card__over-image") },
        showAuthor option userIdSpanMini(post.created.by)(ctx.lang)(cls := "ublog-post-card__over-image")
      )
    )

  def urlOfPost(post: UblogPost.BasePost) = post.blog match {
    case UblogBlog.Id.User(userId) =>
      routes.Ublog.post(usernameOrId(userId), post.slug, post.id.value)
  }

  def editUrlOfPost(post: UblogPost.BasePost) = routes.Ublog.edit(post.id.value)

  private[ublog] def newPostLink(implicit ctx: Context) = ctx.me map { u =>
    a(
      href := routes.Ublog.form(u.username),
      cls := "button button-green",
      dataIcon := "",
      title := trans.ublog.newPost.txt()
    )
  }

  object thumbnail {
    def apply(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
      img(
        cls := "ublog-post-image",
        widthA := size(UblogPost.thumbnail).width,
        heightA := size(UblogPost.thumbnail).height
      )(src := url(post, size))

    def url(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
      post.image match {
        case Some(image) => UblogPost.thumbnail(picfitUrl, image, size)
        case _           => assetUrl("images/user-blog-default.png")
      }
  }
}
