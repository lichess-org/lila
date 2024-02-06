package lila.cms

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.user.Me

final class CmsApi(coll: Coll)(using Executor):

  private given BSONDocumentHandler[CmsPage] = Macros.handler

  def get(id: CmsPage.Id): Fu[Option[CmsPage]] = coll.byId[CmsPage](id)

  def list: Fu[List[CmsPage]] = coll.list[CmsPage]($empty)

  def create(page: CmsPage): Funit = coll.insert.one(page).void

  def update(prev: CmsPage, data: CmsForm.CmsPageData)(using me: Me): Fu[CmsPage] =
    val page = data update me
    coll.update.one($id(prev.id), page) inject page

  def delete(id: CmsPage.Id): Funit = coll.delete.one($id(id)).void
