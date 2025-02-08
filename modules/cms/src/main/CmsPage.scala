package lila.cms

import reactivemongo.api.bson.Macros.Annotations.Key

import scalalib.model.Language
import lila.core.id.{ CmsPageId, CmsPageKey }

case class CmsPage(
    @Key("_id") id: CmsPageId,
    key: CmsPageKey,
    title: String,
    markdown: Markdown,
    language: Language,
    live: Boolean,
    canonicalPath: Option[String],
    by: UserId,
    at: Instant
):
  override def toString = s"CmsPage($id, $key, $language, $title)"

object CmsPage:

  opaque type Key = String
  object Key extends OpaqueString[Key]

  case class Render(page: CmsPage, html: Html):
    export page.*

  case class RenderOpt(key: CmsPageKey, render: Option[Render])
