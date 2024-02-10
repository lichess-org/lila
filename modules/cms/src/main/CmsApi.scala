package lila.cms

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.user.Me
import play.api.mvc.RequestHeader
import lila.i18n.{ Language, LangList, I18nLangPicker }

final class CmsApi(coll: Coll, markup: CmsMarkup)(using Executor):

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
      .map(_.sortLike(LangList.popularLanguages.toVector, _.language))

  def render(key: Key)(req: RequestHeader, userLang: Option[String]): Fu[Option[Render]] =
    getBestFor(key)(req, userLang).flatMapz: page =>
      markup(page).map: html =>
        Render(page, html).some

  def list: Fu[List[CmsPage]] = coll.list[CmsPage]($empty)

  def create(page: CmsPage): Funit = coll.insert.one(page).void

  def update(prev: CmsPage, data: CmsForm.CmsPageData)(using me: Me): Fu[CmsPage] =
    val page = data update me
    val idChange: Funit = (prev.id != page.id).so:
      coll.exists($id(page.id)) flatMap:
        case true  => fufail(s"A page with the ID ${page.id} already exists")
        case false => coll.delete.one($id(prev.id)).void
    idChange >> coll.update.one($id(page.id), page, upsert = true) inject page

  def delete(id: Id): Funit = coll.delete.one($id(id)).void

  private def getBestFor(key: Key)(req: RequestHeader, userLang: Option[String]): Fu[Option[CmsPage]] =
    val prefered = I18nLangPicker.preferedLanguages(req, userLang) :+ lila.i18n.defaultLanguage
    coll
      .list[CmsPage]($doc("key" -> key, "language" $in prefered))
      .map: pages =>
        prefered.foldLeft(none[CmsPage]): (found, language) =>
          found orElse pages.find(_.language == language)
