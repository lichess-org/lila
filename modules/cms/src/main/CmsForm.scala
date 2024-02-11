package lila.cms

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanNonEmptyText, into, slugConstraint, given }
import lila.i18n.{ Language, LangForm }
import lila.user.User

object CmsForm:

  val create = Form:
    mapping(
      "key" -> cleanNonEmptyText(minLength = 3, maxLength = 120).verifying(slugConstraint).into[CmsPage.Key],
      "title"    -> cleanNonEmptyText(minLength = 3, maxLength = 150),
      "markdown" -> cleanNonEmptyText(minLength = 0, maxLength = 1000_000).into[Markdown],
      "language" -> LangForm.popularLanguages.mapping,
      "live"     -> boolean,
      "canonicalPath" -> optional:
        nonEmptyText.transform(p => if p.startsWith("/") then p else s"/$p", identity)
    )(CmsPageData.apply)(unapply)

  def edit(page: CmsPage) = create.fill:
    CmsPageData(
      key = page.key,
      title = page.title,
      markdown = lila.common.MarkdownToastUi.latex.removeFrom(page.markdown),
      language = page.language,
      live = page.live,
      canonicalPath = page.canonicalPath
    )

  case class CmsPageData(
      key: CmsPage.Key,
      title: String,
      markdown: Markdown,
      language: Language,
      live: Boolean,
      canonicalPath: Option[String]
  ):
    def create(user: User) =
      CmsPage(
        id = CmsPage.Id.random,
        key = key,
        title = title,
        markdown = markdown,
        language = language,
        live = live,
        canonicalPath = canonicalPath,
        at = nowInstant,
        by = user.id
      )

    def update(user: User) = create(user)
