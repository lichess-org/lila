package views.html.ublog

import controllers.report.routes.Report as reportRoutes
import controllers.routes
import play.api.mvc.Call

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ublog.{ UblogBlog, UblogPost, UblogRank }

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
        ctx.isAuth.option(jsModule("ublog"))
      ),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${post.title}",
      openGraph = lila.web
        .OpenGraph(
          `type` = "article",
          image = post.image.isDefined.option(thumbnail.url(post, _.Size.Large)),
          title = post.title,
          url = s"$netBaseUrl${routes.Ublog.post(user.username, post.slug, post.id)}",
          description = post.intro
        )
        .some,
      atomLinkTag = link(
        href     := routes.Ublog.userAtom(user.username),
        st.title := trans.ublog.xBlog.txt(user.username)
      ).some,
      robots = netConfig.crawlable && blog.listed && (post.indexable || blog.tier >= UblogRank.Tier.HIGH),
      csp = defaultCsp.withTwitter.withInlineIconFont.some
    ):
      main(cls := "page-menu page-small")(
        menu(Left(user.id)),
        div(cls := "page-menu__content box box-pad ublog-post")(
          post.image.map: image =>
            frag(
              thumbnail(post, _.Size.Large)(cls := "ublog-post__image"),
              image.credit.map { p(cls := "ublog-post__image-credit")(_) }
            ),
          (ctx.is(user) || isGranted(_.ModerateBlog)).option(standardFlash),
          h1(cls := "ublog-post__title")(post.title),
          isGranted(_.ModerateBlog).option(modTools(blog, post)),
          div(cls := "ublog-post__meta")(
            a(
              cls      := userClass(user.id, none, withOnline = true),
              href     := routes.Ublog.index(user.username),
              dataHref := routes.User.show(user.username)
            )(userLinkContent(user)),
            iconTag(Icon.InfoCircle)(
              cls      := "ublog-post__meta__disclaimer",
              st.title := "Opinions expressed by Lichess contributors are their own."
            ),
            post.lived.map: live =>
              span(cls := "ublog-post__meta__date")(semanticDate(live.at)),
            post.live.option(likeButton(post, liked, showText = false)),
            post.live.option(
              span(cls := "ublog-post__views")(
                trans.ublog.nbViews.plural(post.views.value, strong(post.views.value.localize))
              )
            ),
            if ctx.is(user) then
              div(cls := "ublog-post__meta__owner")(
                if post.live then goodTag(trans.ublog.thisPostIsPublished())
                else badTag(trans.ublog.thisIsADraft()),
                " ",
                editButton(post)
              )
            else if isGranted(_.ModerateBlog) then editButton(post)
            else if !post.live then badTag(trans.ublog.thisIsADraft())
            else
              a(
                titleOrText(trans.site.reportXToModerators.txt(user.username)),
                cls := "button button-empty ublog-post__meta__report",
                href := addQueryParams(
                  reportRoutes.form.url,
                  Map(
                    "username" -> user.username,
                    "postUrl"  -> s"$netBaseUrl${urlOfPost(post)}",
                    "reason"   -> "comm"
                  )
                ),
                dataIcon := Icon.CautionTriangle
              )
            ,
            lila.i18n.LangList.nameByStr(post.language)
          ),
          div(cls := "ublog-post__topics")(
            post.topics.map: topic =>
              a(href := routes.Ublog.topic(topic.url, 1))(topic.value)
          ),
          strong(cls := "ublog-post__intro")(post.intro),
          div(cls := "ublog-post__markup expand-text")(markup),
          post.isLichess.option(
            div(cls := "ublog-post__lichess")(
              views.html.base.bits.connectLinks,
              p(a(href := routes.Plan.index)(trans.site.lichessPatronInfo()))
            )
          ),
          div(cls := "ublog-post__footer")(
            (post.live && ~post.discuss).option(
              a(
                href     := routes.Ublog.discuss(post.id),
                cls      := "button text ublog-post__discuss",
                dataIcon := Icon.BubbleConvo
              )(trans.ublog.discussThisBlogPostInTheForum())
            ),
            (ctx.isAuth && ctx.isnt(user)).option(
              div(cls := "ublog-post__actions")(
                likeButton(post, liked, showText = true),
                followable.option(followButton(user, followed))
              )
            ),
            h2(a(href := routes.Ublog.index(user.username))(trans.ublog.moreBlogPostsBy(user.username))),
            (others.size > 0).option(div(cls := "ublog-post-cards")(others.map { card(_) }))
          )
        )
      )

  private def editButton(post: UblogPost)(using PageContext) = a(
    href     := editUrlOfPost(post),
    cls      := "button button-empty text",
    dataIcon := Icon.Pencil
  )(trans.site.edit())

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
      showText.option(
        span(
          cls                      := "button-label",
          attr("data-i18n-like")   := trans.study.like.txt(),
          attr("data-i18n-unlike") := trans.study.unlike.txt()
        )(text)
      )
    )

  private def followButton(user: User, followed: Boolean)(using PageContext) =
    div(
      cls := List(
        "ublog-post__follow" -> true,
        "followed"           -> followed
      )
    ):
      List(
        ("yes", trans.site.unfollowX, routes.Relation.unfollow, Icon.Checkmark),
        ("no", trans.site.followX, routes.Relation.follow, Icon.ThumbsUp)
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
      span(cls := "ublog-post-card__top")(
        thumbnail(post, _.Size.Small)(cls := "ublog-post-card__image"),
        post.lived.map { live => semanticDate(live.at)(cls := "ublog-post-card__over-image") },
        showAuthor match
          case ShowAt.none => emptyFrag
          case showAt =>
            userIdSpanMini(post.created.by)(cls := s"ublog-post-card__over-image pos-$showAt")
      ),
      span(cls := "ublog-post-card__content")(
        h2(cls := "ublog-post-card__title")(post.title),
        showIntro.option(span(cls := "ublog-post-card__intro")(shorten(post.intro, 100)))
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

  private[ublog] def newPostLink(user: User)(using Context) =
    a(
      href     := routes.Ublog.form(user.username),
      cls      := "button button-green",
      dataIcon := Icon.PlusButton,
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

  private def modTools(blog: UblogBlog, post: UblogPost)(using PageContext) =
    env.ublog.rank
      .computeRank(blog, post)
      .map: rank =>
        postForm(cls := "ublog-post__meta", action := routes.Ublog.rankAdjust(post.id))(
          fieldset(cls := "ublog-post__mod-tools")(
            legend(
              span(
                span(
                  label("Rank date:"),
                  if ~post.pinned then "pinned"
                  else span(cls := "ublog-post__meta__date")(semanticDate(rank.value))
                ),
                form3.submit("Submit")(cls := "button-empty")
              )
            ),
            span(
              input(
                tpe   := "checkbox",
                id    := "ublog-post-pinned",
                name  := "pinned",
                value := "true",
                post.pinned.has(true).option(checked)
              ),
              label(`for` := "ublog-post-pinned")(" Pin to top")
            ),
            span(
              "User tier:",
              st.select(name := "tier", cls := "form-control")(UblogRank.Tier.verboseOptions.map:
                (value, name) =>
                  st.option(st.value := value.toString, (blog.tier == value).option(selected))(name)
              )
            ),
            span(
              "Post adjust:",
              input(
                tpe   := "number",
                name  := "days",
                min   := -180,
                max   := 180,
                value := post.rankAdjustDays.so(_.toString)
              ),
              "days"
            )
          )
        )
