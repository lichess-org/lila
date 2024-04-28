package lila.ublog
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import play.api.data.Form
import lila.core.captcha.Captcha

final class UblogFormUi(helpers: Helpers, ui: UblogUi, postUi: UblogPostUi)(
    renderCaptcha: (Form[?], Option[Captcha]) => Context ?=> Frag
):
  import helpers.{ *, given }

  def create(user: User, f: Form[UblogForm.UblogPostData], captcha: Captcha)(using Context) =
    main(cls := "page-menu page-small")(
      ui.menu(Left(user.id)),
      div(cls := "page-menu__content box ublog-post-form")(
        standardFlash,
        boxTop(h1(trans.ublog.newPost())),
        etiquette,
        inner(f, Left(user), captcha.some)
      )
    )

  def edit(post: UblogPost, f: Form[UblogForm.UblogPostData])(using ctx: Context) =
    main(cls := "page-menu page-small")(
      ui.menu(Left(post.created.by)),
      div(cls := "page-menu__content box ublog-post-form")(
        standardFlash,
        boxTop(
          h1(
            if ctx.is(post.created.by) then trans.ublog.editYourBlogPost()
            else s"Edit ${usernameOrId(post.created.by)}'s post"
          ),
          a(href := postUi.urlOfPost(post), dataIcon := Icon.Eye, cls := "text", targetBlank)("Preview")
        ),
        image(post),
        inner(f, Right(post), none),
        postForm(
          cls    := "ublog-post-form__delete",
          action := routes.Ublog.delete(post.id)
        ):
          form3.action:
            submitButton(
              cls   := "button button-red button-empty confirm",
              title := trans.ublog.deleteBlog.txt()
            )(trans.site.delete())
      )
    )

  private def inner(
      form: Form[UblogForm.UblogPostData],
      post: Either[User, UblogPost],
      captcha: Option[Captcha]
  )(using Context) =
    postForm(
      cls    := "form3 ublog-post-form__main",
      action := post.fold(u => routes.Ublog.create(u.username), p => routes.Ublog.update(p.id.value))
    )(
      form3.globalError(form),
      post.toOption.map { p =>
        frag(
          form3.split(
            form3.group(form("imageAlt"), trans.ublog.imageAlt(), half = true)(form3.input(_)),
            form3.group(form("imageCredit"), trans.ublog.imageCredit(), half = true)(form3.input(_))
          )(cls := s"ublog-post-form__image-text ${p.image.isDefined.so("visible")}")
        )
      },
      form3.group(form("title"), trans.ublog.postTitle())(form3.input(_)(autofocus)),
      form3.group(form("intro"), trans.ublog.postIntro())(form3.input(_)(autofocus)),
      form3.group(
        form("markdown"),
        trans.ublog.postBody(),
        help = frag(
          trans.site.embedsAvailable(),
          br,
          tips
        ).some
      ): field =>
        frag(
          form3.textarea(field)(),
          div(cls := "markdown-editor", attr("data-image-upload-url") := routes.Main.uploadImage("ublogBody"))
        ),
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
              form3.group(form("language"), trans.site.language(), half = true):
                form3.select(_, langList.popularLanguagesForm.choices)
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
      renderCaptcha(form, captcha),
      form3.actions(
        a(
          href := post
            .fold(user => routes.Ublog.index(user.username), postUi.urlOfPost)
        )(
          trans.site.cancel()
        ),
        form3.submit((if post.isRight then trans.site.apply else trans.ublog.saveDraft) ())
      )
    )

  private def image(post: UblogPost)(using ctx: Context) =
    div(cls := "ublog-image-edit", data("post-url") := routes.Ublog.image(post.id))(
      postUi.thumbnail(post, _.Size.Small)(
        cls               := "drop-target " + post.image.isDefined.so("user-image"),
        attr("draggable") := "true"
      ),
      div(
        if ctx.is(post.created.by) then
          frag(
            p(strong(trans.ublog.uploadAnImageForYourPost())),
            p(
              trans.ublog.safeToUseImages(),
              fragList(
                List(
                  "unsplash.com"          -> "https://unsplash.com",
                  "commons.wikimedia.org" -> "https://commons.wikimedia.org",
                  "pixabay.com"           -> "https://pixabay.com",
                  "pexels.com"            -> "https://pexels.com",
                  "piqsels.com"           -> "https://piqsels.com",
                  "freeimages.com"        -> "https://freeimages.com"
                ).map: (name, url) =>
                  a(href := url, targetBlank)(name)
              )
            ),
            p(trans.ublog.useImagesYouMadeYourself()),
            p(strong(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB."))),
            form3.file.selectImage()
          )
        else
          postForm(
            cls     := "ublog-post-form__image",
            action  := routes.Ublog.image(post.id),
            enctype := "multipart/form-data"
          )(
            post.image.isDefined.option(submitButton(cls := "button button-red confirm"):
              trans.ublog.deleteImage()
            )
          )
      )
    )

  private def etiquette(using Translate) =
    div(cls := "ublog-post-form__etiquette")(
      p(trans.ublog.safeAndRespectfulContent()),
      p(trans.ublog.inappropriateContentAccountClosed()),
      p(
        a(
          dataIcon := Icon.InfoCircle,
          href     := routes.Cms.lonePage("blog-etiquette"),
          cls      := "text",
          targetBlank
        )("Ranking your blog")
      ),
      p(tips)
    )

  def tips(using Translate) = a(
    dataIcon := Icon.InfoCircle,
    href     := routes.Cms.lonePage("blog-tips"),
    cls      := "text",
    targetBlank
  )(trans.ublog.blogTips())
