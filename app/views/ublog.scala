package views.ublog

import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.i18n.LangList
import lila.core.i18n.Language
import lila.ublog.UblogPost

lazy val ui = lila.ublog.ui.UblogUi(helpers, atomUi)(
  thumbnailUrl = (post, size) =>
    post.image match
      case Some(image) => UblogPost.thumbnail(picfitUrl, image.id, size)
      case _           => assetUrl("images/user-blog-default.png")
)

lazy val post = lila.ublog.ui.UblogPostUi(helpers, ui)(
  ublogRank = env.ublog.rank,
  connectLinks = views.base.bits.connectLinks
)

lazy val form = lila.ublog.ui.UblogFormUi(helpers, ui)(
  renderCaptcha = (form, captcha) =>
    _ ?=>
      captcha.fold(views.base.captcha.hiddenEmpty(form)):
        views.base.captcha(form, _)
)

def community(language: Option[Language], posts: Paginator[UblogPost.PreviewPost])(using ctx: PageContext) =
  val langSelections: List[(String, String)] = ("all", "All languages") ::
    lila.i18n.LangPicker
      .sortFor(LangList.popularNoRegion, ctx.req)
      .map: l =>
        l.language -> LangList.name(l)
  ui.community(language, posts, langSelections)
