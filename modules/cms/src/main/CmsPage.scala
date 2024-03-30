package lila.cms

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.i18n.Language

case class CmsPage(
    @Key("_id") id: CmsPage.Id,
    key: CmsPage.Key,
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

  opaque type Id = String
  object Id extends OpaqueString[Id]:
    def random = Id(scalalib.ThreadLocalRandom.nextString(6))

  opaque type Key = String
  object Key extends OpaqueString[Key]

  case class Render(page: CmsPage, html: Html):
    export page.*
