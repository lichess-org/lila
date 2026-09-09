package lila.ublog
package ui

import play.api.data.Form

import lila.core.captcha.Captcha
import lila.core.id.CmsPageKey
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class UblogFormUi(helpers: Helpers, ui: UblogUi)(
    renderCaptcha: (Form[?], Option[Captcha]) => Context ?=> Frag
):
  import helpers.{ *, given }

  def edit(post: UblogPost, form: Form[UblogForm.UblogPostData], captcha: Option[Captcha])(using
      ctx: Context
  ) =
    val subtitle = if post.isEmpty then trans.ublog.newPost.txt() else post.title;
    Page(s"${trans.ublog.xBlog.txt(titleNameOrId(post.created.by))} • $subtitle")
      .css("bits.ublog.form", "bits.tagify")
      .js(Esm("bits.ublogForm"))
      .js(captcha.isDefined.option(captchaEsm)):
        main(cls := "page-menu page")(
          ui.menu(Left(post.created.by)),
          div(cls := "page-menu__content box ublog-post-form")(
            standardFlash,
            boxTop(
              h1(
                if post.isEmpty then trans.ublog.newPost()
                else if ctx.is(post.created.by) then trans.ublog.editYourBlogPost()
                else s"Edit ${usernameOrId(post.created.by)}'s post"
              )
            ),
            postForm(
              cls := "form3 ublog-post-form__main",
              action := routes.Ublog.postEditForm(post.id)
            )(
              form3.globalError(form),
              div(cls := "form-group etiquette-and-image")(
                etiquette,
                image(post, form)
              ),
              form3.group(form("title"), trans.ublog.postTitle())(form3.input(_)(autofocus)),
              form3.group(form("intro"), trans.ublog.postIntro())(form3.input(_)(autofocus)),
              div(cls := "form-group")(
                bits.markdownEditor(MarkdownRealm.blog)(
                  form3.textarea(form("markdown"))(autocomplete := "off")
                )
              ),
              div(
                form3.split(
                  form3.group(form("topics"), frag(trans.ublog.selectPostTopics()), half = true)(
                    form3.textarea(_)(dataRel := UblogTopic.all.mkString(","))
                  ),
                  form3.group(form("language"), trans.site.language(), half = true):
                    form3.select(_, langList.popularLanguagesForm.choices)
                ),
                form3.split(
                  form3.checkboxGroup(
                    form("discuss"),
                    trans.ublog.createBlogDiscussion(),
                    help = trans.ublog.createBlogDiscussionHelp().some,
                    half = true
                  ),
                  form3.checkboxGroup(
                    form("live"),
                    trans.ublog.publishOnYourBlog(),
                    help = trans.ublog.publishHelp().some,
                    half = true
                  )
                ),
                form3.split(
                  form3.checkboxGroup(
                    form("sticky"),
                    trans.ublog.stickyPost(),
                    help = trans.ublog.stickyPostHelp().some,
                    half = true
                  ),
                  form3.checkboxGroup(
                    form("ads"),
                    "Includes promoted/sponsored content or referral links",
                    help = ads.some,
                    half = true
                  )
                )
              ),
              renderCaptcha(form, captcha),
              form3.actions(
                a(href := ui.urlOfPost(post))(trans.site.cancel()),
                form3.submit(trans.site.apply())
              )
            ),
            (!post.isEmpty).option(
              postForm(
                cls := "ublog-post-form__delete",
                action := routes.Ublog.delete(post.id),
                enctype := "multipart/form-data"
              ):
                form3.action:
                  submitButton(
                    cls := "button button-red button-empty yes-no-confirm",
                    title := trans.ublog.deleteBlog.txt()
                  )(trans.site.delete())
            )
          )
        )

  private def image(post: UblogPost, form: Form[UblogForm.UblogPostData])(using ctx: Context) =
    form3.fieldset("Cover image")(
      div(cls := "ublog-image-edit", data("post-url") := routes.Ublog.image(post.id))(
        ui.thumbnail(post, _.Size.Large)(
          cls := "drop-target " + post.image.isDefined.so("user-image"),
          attr("draggable") := "true"
        ),
        ctx
          .is(post.created.by)
          .option(
            span(
              if post.image.isDefined then
                frag(
                  div(label(cls := "form-label")(trans.ublog.imageAlt()), form3.input(form("imageAlt"))),
                  div(label(cls := "form-label")(trans.ublog.imageCredit()), form3.input(form("imageCredit")))
                )

              else p(strong(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB."))),
              form3.file.selectImage()
            )
          )
      )
    )

  private def etiquette(using Translate) =
    form3.fieldset("Etiquette")(
      p(trans.ublog.safeAndRespectfulContent()),
      p(trans.ublog.noCopyrightedImages()),
      p(trans.ublog.inappropriateContentAccountClosed()),
      p(
        a(
          dataIcon := Icon.InfoCircle,
          href := routes.Cms.lonePage(CmsPageKey("blog-etiquette")),
          cls := "text",
          targetBlank
        )("Ranking your blog")
      ),
      p(tips)
    )

  def tips(using Translate) = a(
    dataIcon := Icon.InfoCircle,
    href := routes.Cms.lonePage(CmsPageKey("blog-tips")),
    cls := "text",
    targetBlank
  )(trans.ublog.blogTips())

  val ads = a(
    dataIcon := Icon.InfoCircle,
    href := routes.Cms.lonePage(CmsPageKey("blog-etiquette")),
    cls := "text",
    targetBlank
  )("Mandatory for sponsored content, affiliate links or commercial advertisement")
