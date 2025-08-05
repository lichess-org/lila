package lila.cms

import play.api.data.*
import play.api.data.Forms.*
import scalalib.model.Language

import lila.common.Form.{ cleanNonEmptyText, cleanTextWithSymbols, into, slugConstraint }
import lila.core.i18n.LangList
import lila.core.id.{ CmsPageId, CmsPageKey }

final class CmsForm(langList: LangList):

  val create = Form:
    mapping(
      "key" -> cleanNonEmptyText(minLength = 3, maxLength = 120).verifying(slugConstraint).into[CmsPageKey],
      "title" -> cleanNonEmptyText(minLength = 3, maxLength = 150),
      "markdown" -> cleanTextWithSymbols(maxLength = 1000_000).into[Markdown],
      "language" -> langList.popularLanguagesForm.mapping,
      "live" -> boolean,
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
      key: CmsPageKey,
      title: String,
      markdown: Markdown,
      language: Language,
      live: Boolean,
      canonicalPath: Option[String]
  ):
    def create(user: UserId) =
      CmsPage(
        id = CmsPageId(scalalib.ThreadLocalRandom.nextString(6)),
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
