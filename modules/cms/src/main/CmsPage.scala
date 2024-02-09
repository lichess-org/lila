package lila.cms

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.i18n.Language

case class CmsPage(
    @Key("_id") id: CmsPage.Id,
    title: String,
    markdown: Markdown,
    language: Language,
    live: Boolean,
    canonicalPath: Option[String],
    by: UserId,
    at: Instant
)

object CmsPage:

  opaque type Id = String
  object Id extends TotalWrapper[Id, String]

  case class Render(page: CmsPage, html: Html):
    export page.*
