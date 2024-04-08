package lila.cms

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.validation.Constraints

import lila.common.Form.{ cleanNonEmptyText, cleanTextWithSymbols, into, slugConstraint }
import lila.core.i18n.{ LangList, Language }

final class CmsForm(langList: LangList):

  val create = Form:
    mapping(
      "key" -> cleanNonEmptyText(minLength = 3, maxLength = 120).verifying(slugConstraint).into[CmsPage.Key],
      "title" -> cleanNonEmptyText(minLength = 3, maxLength = 150),
      "markdown" -> cleanTextWithSymbols
        .verifying(Constraints.minLength(0), Constraints.maxLength(1000_000))
        .into[Markdown],
      "language" -> langList.popularLanguagesForm.mapping,
      "live"     -> boolean,
      "canonicalPath" -> optional:
        nonEmptyText.transform(p => if p.startsWith("/") then p else s"/$p", identity)
    )(CmsForm.CmsPageData.apply)(unapply)

  def edit(page: CmsPage) = create.fill:
    CmsForm.CmsPageData(
      key = page.key,
      title = page.title,
      markdown = lila.common.MarkdownToastUi.latex.removeFrom(page.markdown),
      language = page.language,
      live = page.live,
      canonicalPath = page.canonicalPath
    )

object CmsForm:
  case class CmsPageData(
      key: CmsPage.Key,
      title: String,
      markdown: Markdown,
      language: Language,
      live: Boolean,
      canonicalPath: Option[String]
  ):
    def create(user: UserId) =
      CmsPage(
        id = CmsPage.Id.random,
        key = key,
        title = title,
        markdown = markdown,
        language = language,
        live = live,
        canonicalPath = canonicalPath,
        at = nowInstant,
        by = user
      )

    def update(prev: CmsPage, user: UserId) = create(user).copy(id = prev.id)
