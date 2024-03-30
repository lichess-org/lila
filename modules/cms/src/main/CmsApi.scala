package lila.cms

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.core.i18n.{ LangPicker, LangList, Language, defaultLanguage }
import lila.user.Me

final class CmsApi(coll: Coll, markup: CmsMarkup, langList: LangList, langPicker: LangPicker)(using Executor):

  private given BSONDocumentHandler[CmsPage] = Macros.handler

  import CmsPage.*

  def get(id: Id): Fu[Option[CmsPage]] = coll.byId[CmsPage](id)

  def get(key: Key, lang: Language): Fu[Option[CmsPage]] =
    coll.one[CmsPage]($doc("key" -> key, "language" -> lang))

  def withAlternatives(id: Id): Fu[Option[NonEmptyList[CmsPage]]] =
    get(id).flatMapz: page =>
      getAlternatives(page.key).map: alts =>
        NonEmptyList(page, alts.filter(_.id != id)).some

  def getAlternatives(key: Key): Fu[List[CmsPage]] =
    coll
      .list[CmsPage]($doc("key" -> key))
      .map(_.sortLike(langList.popularLanguages.toVector, _.language))

  def render(key: Key)(req: RequestHeader, prefLang: Lang): Fu[Option[Render]] =
    getBestFor(key)(req, prefLang).flatMapz: page =>
      markup(page).map: html =>
        Render(page, html).some

  def list: Fu[List[CmsPage]] = coll.list[CmsPage]($empty)

  def create(page: CmsPage): Funit = coll.insert.one(page).void

  def update(prev: CmsPage, data: CmsForm.CmsPageData)(using me: Me): Fu[CmsPage] =
    val page = data.update(prev, me)
    coll.update.one($id(page.id), page).inject(page)

  def delete(id: Id): Funit = coll.delete.one($id(id)).void

  private def getBestFor(key: Key)(req: RequestHeader, prefLang: Lang): Fu[Option[CmsPage]] =
    val prefered = langPicker.preferedLanguages(req, prefLang) :+ defaultLanguage
    coll
      .list[CmsPage]($doc("key" -> key, "language".$in(prefered)))
      .map: pages =>
        prefered.foldLeft(none[CmsPage]): (found, language) =>
          found.orElse(pages.find(_.language == language))
