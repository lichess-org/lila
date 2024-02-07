package lila.cms

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ cleanNonEmptyText, into, slugConstraint, given }
import lila.i18n.{ Language, LangForm }
import lila.user.User

object CmsForm:

  val create = Form:
    mapping(
      "id"    -> cleanNonEmptyText(minLength = 3, maxLength = 120).verifying(slugConstraint).into[CmsPage.Id],
      "title" -> cleanNonEmptyText(minLength = 3, maxLength = 150),
      "markdown"      -> cleanNonEmptyText(minLength = 0, maxLength = 1000_000).into[Markdown],
      "language"      -> LangForm.popularLanguages.mapping,
      "live"          -> boolean,
      "canonicalPath" -> optional(nonEmptyText)
    )(CmsPageData.apply)(unapply)

  def edit(page: CmsPage) = create.fill:
    CmsPageData(
      id = page.id,
      title = page.title,
      markdown = lila.common.MarkdownToastUi.latex.removeFrom(page.markdown),
      language = page.language,
      live = page.live,
      canonicalPath = page.canonicalPath
    )

  case class CmsPageData(
      id: CmsPage.Id,
      title: String,
      markdown: Markdown,
      language: Language,
      live: Boolean,
      canonicalPath: Option[String]
  ):
    def create(user: User) =
      CmsPage(
        id = id,
        title = title,
        markdown = markdown,
        language = language,
        live = live,
        canonicalPath = canonicalPath,
        at = nowInstant,
        by = user.id
      )

    def update(user: User) = create(user)
