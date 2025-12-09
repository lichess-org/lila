package lila.cms

import play.api.i18n.Lang
import reactivemongo.api.bson.*
import scalalib.model.Language

import lila.core.i18n.{ LangList, LangPicker, defaultLanguage, toLanguage }
import lila.core.id.{ CmsPageId, CmsPageKey }
import lila.db.dsl.{ *, given }
import lila.ui.Context

final class CmsApi(coll: Coll, markdown: lila.memo.MarkdownCache, langList: LangList, langPicker: LangPicker)(
    using Executor
):

  private given BSONDocumentHandler[CmsPage] = Macros.handler

  import CmsPage.*

  def get(id: CmsPageId): Fu[Option[CmsPage]] = coll.byId[CmsPage](id)

  def get(key: CmsPageKey, lang: Language): Fu[Option[CmsPage]] =
    coll.one[CmsPage]($doc("key" -> key, "language" -> lang))

  def withAlternatives(id: CmsPageId): Fu[Option[NonEmptyList[CmsPage]]] =
    get(id).flatMapz: page =>
      getAlternatives(page.key).map: alts =>
        NonEmptyList(page, alts.filter(_.id != id)).some

  def getAlternatives(key: CmsPageKey): Fu[List[CmsPage]] =
    coll
      .list[CmsPage]($doc("key" -> key))
      .map(_.sortLike(langList.popularLanguages.toVector, _.language))

  def render(key: CmsPageKey, liveCheck: Boolean = false)(using Context): Fu[Option[Render]] =
    getBestFor(key).flatMap:
      _.filter(_.live || !liveCheck).so: page =>
        markdown
          .toHtml(s"cms:${page.id}", page.markdown, lila.cms.markdownOptions)
          .map(Render(page, _).some)

  def renderOpt(key: CmsPageKey)(using Context): Fu[RenderOpt] =
    render(key).map(RenderOpt(key, _))

  def list: Fu[List[CmsPage]] = coll.list[CmsPage]($empty)

  def create(page: CmsPage): Funit = coll.insert.one(page).void

  def update(prev: CmsPage, data: CmsForm.CmsPageData)(using me: MyId): Fu[CmsPage] =
    val page = data.update(prev, me)
    coll.update.one($id(page.id), page).inject(page)

  def delete(id: CmsPageId): Funit = coll.delete.one($id(id)).void

  private def getBestFor(key: CmsPageKey)(using ctx: Context): Fu[Option[CmsPage]] =
    val queryLang = ctx.req.getQueryString("lang").flatMap(Lang.get).map(toLanguage)
    val prefered = queryLang match
      case Some(query) => List(query)
      case None => langPicker.preferedLanguages(ctx.req, ctx.lang) :+ defaultLanguage
    coll
      .list[CmsPage]($doc("key" -> key, "language".$in(prefered)))
      .map: pages =>
        prefered.collectFirstSome: language =>
          pages.find(_.language == language)
