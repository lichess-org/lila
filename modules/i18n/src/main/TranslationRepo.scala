package lila.i18n

import play.api.libs.json.Json

import lila.db.api._
import lila.db.Implicits._
import tube.translationTube

private[i18n] object TranslationRepo {

  type ID = Int

  def nextId: Fu[ID] = $primitive.one(
    $select.all,
    "_id",
    _ sort $sort.descId
  )(_.asOpt[Int]) map (opt => ~opt + 1)

  def findFrom(id: ID): Fu[List[Translation]] =
    $find($query(Json.obj("_id" -> $gte(id))) sort $sort.ascId)
}
