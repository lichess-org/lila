package views.html.ublog

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.Captcha
import lila.i18n.LangList
import lila.ublog.UblogForm.UblogPostData
import lila.ublog.{ UblogPost, UblogTopic }
import lila.user.User

object form:

  import views.html.ublog.{ post as postView }

  private def moreCss(implicit ctx: Context) = frag(cssTag("ublog.form"), cssTag("tagify"))

  def create(user: User, f: Form[UblogPostData], captcha: Captcha)(implicit ctx: Context) =
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

  def edit(post: UblogPost, f: Form[UblogPostData])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = moreCss,
      moreJs = jsModule("ublogForm"),
      title = s"${trans.ublog.xBlog.txt(titleNameOrId(post.created.by))} blog • ${post.title}"
    ) {
      main(cls := "page-menu page-small")(
        views.html.blog.bits.menu(none, "mine".some),
        div(cls := "page-menu__content box ublog-post-form")(
          standardFlash,
          boxTop(
            h1(
              if (ctx isUserId post.created.by) trans.ublog.editYourBlogPost()
              else s"Edit ${usernameOrId(post.created.by)}'s post"
            ),
            a(href := postView.urlOfPost(post), dataIcon := "", cls := "text", targetBlank)("Preview")
          ),
          imageForm(post),
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

  private def imageForm(post: UblogPost)(implicit ctx: Context) =
    postForm(
      cls     := "ublog-post-form__image",
      action  := routes.Ublog.image(post.id),
      enctype := "multipart/form-data"
    )(
      form3.split(
        div(cls := "form-group form-half")(formImage(post)),
        div(cls := "form-group form-half")(
          if (ctx isUserId post.created.by)
            frag(
              p(trans.ublog.uploadAnImageForYourPost()),
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
                  ).map { case (name, url) =>
                    a(href := url, targetBlank)(name)
                  }
                )
              ),
              p(
                trans.ublog.useImagesYouMadeYourself()
              ),
              p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB.")),
              form3.file.image("image")
            )
          else
            post.image.isDefined option submitButton(cls := "button button-red confirm")(
              trans.ublog.deleteImage()
            )
        )
      )
    )

  def formImage(post: UblogPost) =
    postView.thumbnail(post, _.Small)(cls := post.image.isDefined.option("user-image"))

  private def inner(form: Form[UblogPostData], post: Either[User, UblogPost], captcha: Option[Captcha])(
      implicit ctx: Context
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
          )(cls := s"ublog-post-form__image-text ${p.image.isDefined ?? "visible"}")
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
          div(id := "markdown-editor")
        )
      },
      post.toOption match {
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
              form3.group(form("language"), trans.language(), half = true) { field =>
                form3.select(
                  field,
                  LangList.popularNoRegion.map { l =>
                    l.code -> l.toLocale.getDisplayLanguage
                  }
                )
              }
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
      },
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
        form3.submit((if (post.isRight) trans.apply else trans.ublog.saveDraft) ())
      )
    )

  private def etiquette(implicit ctx: Context) = div(cls := "ublog-post-form__etiquette")(
    p(trans.ublog.safeAndRespectfulContent()),
    p(trans.ublog.inappropriateContentAccountClosed()),
    p(
      a(
        dataIcon := "",
        href     := routes.Page.loneBookmark("blog-etiquette"),
        cls      := "text",
        targetBlank
      )("Blog Etiquette")
    ),
    p(tips)
  )

  def tips(implicit ctx: Context) = a(
    dataIcon := "",
    href     := routes.Page.loneBookmark("blog-tips"),
    cls      := "text",
    targetBlank
  )(trans.ublog.blogTips())
