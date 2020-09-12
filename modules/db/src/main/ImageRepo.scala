package lila.db

import DbImage.DbImageBSONHandler

import lila.db.dsl._

final class ImageRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  def fetch(id: String): Fu[Option[DbImage]] = coll.byId[DbImage](id)

  def save(image: DbImage): Funit =
    coll.update.one($id(image.id), image, upsert = true).void
}
