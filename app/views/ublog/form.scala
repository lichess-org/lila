package views.html.ublog

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.Captcha
import lila.ublog.UblogForm.UblogPostData
import lila.ublog.{ UblogPost, UblogTopic }
import lila.user.User

object form:

  import views.html.ublog.{ post as postView }

  private def moreCss(using PageContext) = frag(cssTag("ublog.form"), cssTag("tagify"))

  def create(user: User, f: Form[UblogPostData], captcha: Captcha)(using PageContext) =
    views.html.base.layout(
      moreCss = moreCss,
      moreJs = frag(jsModule("ublogForm"), captchaTag),
      title = s"${trans.ublog.xBlog.txt(user.username)} • ${trans.ublog.newPost.txt()}"
    ) {
      main(cls := "page-menu page-small")(
        views.html.blog.bits.menu(none, "mine".some),
        div(cls := "page-menu__content box ublog-post-form")(
          standardFlash,
          boxTop(h1(trans.ublog.newPost())),
          etiquette,
          inner(f, Left(user), captcha.some)
        )
      )
    }

  def edit(post: UblogPost, f: Form[UblogPostData])(using ctx: PageContext) =
    views.html.base.layout(
      moreCss = moreCss,
      moreJs = jsModule("ublogForm"),
      title = s"${trans.ublog.xBlog.txt(titleNameOrId(post.created.by))} • ${post.title}"
    ) {
      main(cls := "page-menu page-small")(
        views.html.blog.bits.menu(none, "mine".some),
        div(cls := "page-menu__content box ublog-post-form")(
          standardFlash,
          boxTop(
            h1(
              if ctx is post.created.by then trans.ublog.editYourBlogPost()
              else s"Edit ${usernameOrId(post.created.by)}'s post"
            ),
            a(href := postView.urlOfPost(post), dataIcon := licon.Eye, cls := "text", targetBlank)("Preview")
          ),
          image(post),
          inner(f, Right(post), none),
          postForm(
            cls    := "ublog-post-form__delete",
            action := routes.Ublog.delete(post.id)
          )(
            form3.action(
              submitButton(
                cls   := "button button-red button-empty confirm",
                title := trans.ublog.deleteBlog.txt()
              )(trans.delete())
            )
          )
        )
      )
    }

  private def image(post: UblogPost)(using ctx: PageContext) =
    div(cls := "ublog-image-edit", data("post-url") := routes.Ublog.image(post.id))(
      postView.thumbnail(post, _.Size.Small)(
        cls               := "drop-target " + post.image.isDefined.so("user-image"),
        attr("draggable") := "true"
      ),
      div(
        if ctx is post.created.by then
          frag(
            p(strong(trans.ublog.uploadAnImageForYourPost())),
            p(
              trans.ublog.safeToUseImages(),
              fragList(
                List(
                  "unsplash.com"        -> "https://unsplash.com",
                  "creativecommons.org" -> "https://search.creativecommons.org",
                  "pixabay.com"         -> "https://pixabay.com",
                  "pexels.com"          -> "https://pexels.com",
                  "piqsels.com"         -> "https://piqsels.com",
                  "freeimages.com"      -> "https://freeimages.com"
                ).map: (name, url) =>
                  a(href := url, targetBlank)(name)
              )
            ),
            p(trans.ublog.useImagesYouMadeYourself()),
            p(strong(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB."))),
            button(cls := "button select-image")(s"select image")
          )
        else
          postForm(
            cls     := "ublog-post-form__image",
            action  := routes.Ublog.image(post.id),
            enctype := "multipart/form-data"
          )(
            post.image.isDefined option submitButton(cls := "button button-red confirm"):
              trans.ublog.deleteImage()
          )
      )
    )

  private def inner(form: Form[UblogPostData], post: Either[User, UblogPost], captcha: Option[Captcha])(using
      PageContext
  ) =
    postForm(
      cls    := "form3 ublog-post-form__main",
      action := post.fold(_ => routes.Ublog.create, p => routes.Ublog.update(p.id.value))
    )(
      form3.globalError(form),
      post.toOption.map { p =>
        frag(
          form3.split(
            form3.group(form("imageAlt"), trans.ublog.imageAlt(), half = true)(form3.input(_)),
            form3.group(form("imageCredit"), trans.ublog.imageCredit(), half = true)(form3.input(_))
          )(cls := s"ublog-post-form__image-text ${p.image.isDefined so "visible"}")
        )
      },
      form3.group(form("title"), trans.ublog.postTitle())(form3.input(_)(autofocus)),
      form3.group(form("intro"), trans.ublog.postIntro())(form3.input(_)(autofocus)),
      form3.group(
        form("markdown"),
        trans.ublog.postBody(),
        help = frag(
          trans.embedsAvailable(),
          br,
          tips
        ).some
      ) { field =>
        frag(
          form3.textarea(field)(),
          div(id := "markdown-editor", attr("data-image-upload-url") := routes.Main.uploadImage("ublogBody"))
        )
      },
      post.toOption match
        case None =>
          form3.group(form("topics"), frag(trans.ublog.selectPostTopics()))(
            form3.textarea(_)(dataRel := UblogTopic.all.mkString(","))
          )
        case _ =>
          div(
            form3.split(
              form3.group(form("topics"), frag(trans.ublog.selectPostTopics()), half = true)(
                form3.textarea(_)(dataRel := UblogTopic.all.mkString(","))
              ),
              form3.group(form("language"), trans.language(), half = true):
                form3.select(_, lila.i18n.LangForm.popularLanguages.choices)
            ),
            form3.split(
              form3.checkbox(
                form("discuss"),
                trans.ublog.createBlogDiscussion(),
                help = trans.ublog.createBlogDiscussionHelp().some,
                half = true
              ),
              form3.checkbox(
                form("live"),
                trans.ublog.publishOnYourBlog(),
                help = trans.ublog.publishHelp().some,
                half = true
              )
            )
          )
      ,
      captcha.fold(views.html.base.captcha.hiddenEmpty(form)) { c =>
        views.html.base.captcha(form, c)
      },
      form3.actions(
        a(
          href := post
            .fold(user => routes.Ublog.index(user.username), views.html.ublog.post.urlOfPost)
        )(
          trans.cancel()
        ),
        form3.submit((if post.isRight then trans.apply else trans.ublog.saveDraft) ())
      )
    )

  private def etiquette(using PageContext) = div(cls := "ublog-post-form__etiquette")(
    p(trans.ublog.safeAndRespectfulContent()),
    p(trans.ublog.inappropriateContentAccountClosed()),
    p(
      a(
        dataIcon := licon.InfoCircle,
        href     := routes.ContentPage.loneBookmark("blog-etiquette"),
        cls      := "text",
        targetBlank
      )("Ranking your blog")
    ),
    p(tips)
  )

  def tips(using PageContext) = a(
    dataIcon := licon.InfoCircle,
    href     := routes.ContentPage.loneBookmark("blog-tips"),
    cls      := "text",
    targetBlank
  )(trans.ublog.blogTips())
