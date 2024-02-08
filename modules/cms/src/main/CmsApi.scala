package lila.cms

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.user.Me

final class CmsApi(coll: Coll, markup: CmsMarkup)(using Executor):

  private given BSONDocumentHandler[CmsPage] = Macros.handler

  def get(id: CmsPage.Id): Fu[Option[CmsPage]] = coll.byId[CmsPage](id)

  def render(id: CmsPage.Id): Fu[Option[CmsPage.Render]] = get(id).flatMapz: page =>
    markup(page).map: html =>
      CmsPage.Render(page, html).some

  def list: Fu[List[CmsPage]] = coll.list[CmsPage]($empty)

  def create(page: CmsPage): Funit = coll.insert.one(page).void

  def update(prev: CmsPage, data: CmsForm.CmsPageData)(using me: Me): Fu[CmsPage] =
    val page = data update me
    val idChange: Funit = (prev.id != page.id).so:
      coll.exists($id(page.id)) flatMap:
        case true  => fufail(s"A page with the ID ${page.id} already exists")
        case false => coll.delete.one($id(prev.id)).void
    idChange >> coll.update.one($id(page.id), page, upsert = true) inject page

  def delete(id: CmsPage.Id): Funit = coll.delete.one($id(id)).void
