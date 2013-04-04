package lila.i18n

import lila.db.api._
import lila.db.Implicits._
import tube.translationTube

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

private[i18n] object TranslationRepo {

  type ID = Int

  val nextId: Fu[ID] = $primitive.one(
    $select.all,
    "_id",
    _ sort $sort.descId
  )(_.asOpt[Int]) map (opt ⇒ ~opt + 1)

  def findFrom(id: ID): Fu[List[Translation]] =
    $find($query(Json.obj("_id" -> $lte(id))) sort $sort.ascId)
}
