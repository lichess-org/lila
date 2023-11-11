package views.html.ublog

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import play.api.mvc.Call

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.ublog.{ UblogBlog, UblogPost }
import lila.user.User

object post:

  def apply(
      user: User,
      blog: UblogBlog,
      post: UblogPost,
      markup: Frag,
      others: List[UblogPost.PreviewPost],
      liked: Boolean,
      followable: Boolean,
      followed: Boolean
  )(using ctx: PageContext) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = frag(
        jsModule("expandText"),
        ctx.isAuth option jsModule("ublog")
      ),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${post.title}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = post.image.isDefined option thumbnail.url(post, _.Size.Large),
          title = post.title,
          url = s"$netBaseUrl${routes.Ublog.post(user.username, post.slug, post.id)}",
          description = post.intro
        )
        .some,
      atomLinkTag = link(
        href     := routes.Ublog.userAtom(user.username),
        st.title := trans.ublog.xBlog.txt(user.username)
      ).some,
      robots = netConfig.crawlable && blog.listed && (post.indexable || blog.tier >= UblogBlog.Tier.HIGH),
      csp = defaultCsp.withTwitter.withInlineIconFont.some
    ):
      main(cls := "page-menu page-small")(
        views.html.blog.bits.menu(none, (if ctx is user then "mine" else "community").some),
        div(cls := "page-menu__content box box-pad ublog-post")(
          post.image.map: image =>
            frag(
              thumbnail(post, _.Size.Large)(cls := "ublog-post__image"),
              image.credit.map { p(cls := "ublog-post__image-credit")(_) }
            ),
          ctx.is(user) || isGranted(_.ModerateBlog) option standardFlash,
          h1(cls := "ublog-post__title")(post.title),
          div(cls := "ublog-post__meta")(
            a(
              href     := routes.Ublog.index(user.username),
              cls      := userClass(user.id, none, withOnline = true),
              dataHref := routes.User.show(user.username)
            )(
              lineIcon(user),
              titleTag(user.title),
              user.username,
              !ctx.is(user) && isGranted(_.ModerateBlog) option
                (if blog.tier <= UblogBlog.Tier.VISIBLE then badTag else goodTag) (
                  cls := "ublog-post__tier"
                )(UblogBlog.Tier.name(blog.tier))
            ),
            iconTag(licon.InfoCircle)(
              cls      := "ublog-post__meta__disclaimer",
              st.title := "Opinions expressed by Lichess contributors are their own."
            ),
            post.lived.map: live =>
              span(cls := "ublog-post__meta__date")(semanticDate(live.at)),
            likeButton(post, liked, showText = false),
            span(cls := "ublog-post__views")(
              trans.ublog.nbViews.plural(post.views.value, strong(post.views.value.localize))
            ),
            if ctx is user then
              div(cls := "ublog-post__meta__owner")(
                (if post.live then goodTag else badTag):
                  if post.live then trans.ublog.thisPostIsPublished() else trans.ublog.thisIsADraft()
                ,
                " ",
                editButton(post)
              )
            else if isGranted(_.ModerateBlog) then editButton(post)
            else
              a(
                titleOrText(trans.reportXToModerators.txt(user.username)),
                cls := "button button-empty ublog-post__meta__report",
                href := addQueryParams(
                  reportRoutes.form.url,
                  Map(
                    "username" -> user.username,
                    "postUrl"  -> s"$netBaseUrl${urlOfPost(post)}",
                    "reason"   -> "comm"
                  )
                ),
                dataIcon := licon.CautionTriangle
              )
          ),
          div(cls := "ublog-post__topics")(
            post.topics.map: topic =>
              a(href := routes.Ublog.topic(topic.url, 1))(topic.value)
          ),
          strong(cls := "ublog-post__intro")(post.intro),
          div(cls := "ublog-post__markup expand-text")(markup),
          div(cls := "ublog-post__footer")(
            post.live && ~post.discuss option a(
              href     := routes.Ublog.discuss(post.id),
              cls      := "button text ublog-post__discuss",
              dataIcon := licon.BubbleConvo
            )(trans.ublog.discussThisBlogPostInTheForum()),
            (ctx.isAuth && !ctx.is(user)) option
              div(cls := "ublog-post__actions")(
                likeButton(post, liked, showText = true),
                followable option followButton(user, followed)
              ),
            h2(a(href := routes.Ublog.index(user.username))(trans.ublog.moreBlogPostsBy(user.username))),
            others.size > 0 option div(cls := "ublog-post-cards")(others map { card(_) })
          )
        )
      )

  private def editButton(post: UblogPost)(using PageContext) = a(
    href     := editUrlOfPost(post),
    cls      := "button button-empty text",
    dataIcon := licon.Pencil
  )(trans.edit())

  private def likeButton(post: UblogPost, liked: Boolean, showText: Boolean)(using PageContext) =
    val text = if liked then trans.study.unlike.txt() else trans.study.like.txt()
    button(
      tpe := "button",
      cls := List(
        "ublog-post__like is"                                -> true,
        "ublog-post__like--liked"                            -> liked,
        "ublog-post__like--big button button-big button-red" -> showText,
        "ublog-post__like--mini button-link"                 -> !showText
      ),
      dataRel := post.id,
      title   := text
    )(
      span(cls := "ublog-post__like__nb")(post.likes.value.localize),
      showText option span(
        cls                      := "button-label",
        attr("data-i18n-like")   := trans.study.like.txt(),
        attr("data-i18n-unlike") := trans.study.unlike.txt()
      )(text)
    )

  private def followButton(user: User, followed: Boolean)(using PageContext) =
    div(
      cls := List(
        "ublog-post__follow" -> true,
        "followed"           -> followed
      )
    ):
      List(
        ("yes", trans.unfollowX, routes.Relation.unfollow, licon.Checkmark),
        ("no", trans.followX, routes.Relation.follow, licon.ThumbsUp)
      ).map: (role, text, route, icon) =>
        button(
          cls      := s"ublog-post__follow__$role button button-big",
          dataIcon := icon,
          dataRel  := route(user.id)
        )(
          span(cls := "button-label")(text(user.titleUsername))
        )

  enum ShowAt:
    case top, bottom, none

  def card(
      post: UblogPost.BasePost,
      makeUrl: UblogPost.BasePost => Call = urlOfPost,
      showAuthor: ShowAt = ShowAt.none,
      showIntro: Boolean = true
  )(using Context) =
    a(cls := "ublog-post-card ublog-post-card--link", href := makeUrl(post))(
      div(style := "position: relative")(
        thumbnail(post, _.Size.Small)(cls := "ublog-post-card__image"),
        post.lived map { live => semanticDate(live.at)(cls := "ublog-post-card__over-image") },
        showAuthor match
          case ShowAt.none => emptyFrag
          case showAt =>
            userIdSpanMini(post.created.by)(cls := s"ublog-post-card__over-image pos-$showAt")
      ),
      span(cls := "ublog-post-card__content")(
        h2(cls := "ublog-post-card__title")(post.title),
        showIntro option span(cls := "ublog-post-card__intro")(shorten(post.intro, 100))
      )
    )

  def miniCard(post: UblogPost.BasePost) =
    span(cls := "ublog-post-card ublog-post-card--mini")(
      thumbnail(post, _.Size.Small)(cls := "ublog-post-card__image"),
      h3(cls := "ublog-post-card__title")(post.title)
    )

  def urlOfPost(post: UblogPost.BasePost) = post.blog match
    case UblogBlog.Id.User(userId) =>
      routes.Ublog.post(usernameOrId(userId), post.slug, post.id)

  def editUrlOfPost(post: UblogPost.BasePost) = routes.Ublog.edit(post.id)

  private[ublog] def newPostLink(using ctx: Context) = ctx.me.map: u =>
    a(
      href     := routes.Ublog.form(u.username),
      cls      := "button button-green",
      dataIcon := licon.PlusButton,
      title    := trans.ublog.newPost.txt()
    )

  object thumbnail:
    def apply(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
      img(
        cls     := "ublog-post-image",
        widthA  := size(UblogPost.thumbnail).width,
        heightA := size(UblogPost.thumbnail).height,
        alt     := post.image.flatMap(_.alt)
      )(src := url(post, size))

    def url(post: UblogPost.BasePost, size: UblogPost.thumbnail.SizeSelector) =
      post.image match
        case Some(image) => UblogPost.thumbnail(picfitUrl, image.id, size)
        case _           => assetUrl("images/user-blog-default.png")
