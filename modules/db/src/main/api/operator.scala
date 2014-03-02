package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson._

object $operator extends $operator
trait $operator {

  def $set[A: Writes](pairs: (String, A)*) = Json.obj("$set" -> Json.obj(wrap(pairs): _*))
  def $set(pairs: (String, Json.JsValueWrapper)*) = Json.obj("$set" -> Json.obj(pairs: _*))
  def $set(pairs: JsObject) = Json.obj("$set" -> pairs)
  def $setBson(pairs: (String, BSONValue)*) = BSONDocument("$set" -> BSONDocument(pairs))
  def $setBson(pairs: BSONDocument) = BSONDocument("$set" -> pairs)
  def $unset(fields: String*) = Json.obj("$unset" -> Json.obj(wrap(fields map (_ -> true)): _*))
  def $inc[A: Writes](pairs: (String, A)*) = Json.obj("$inc" -> Json.obj(wrap(pairs): _*))
  def $incBson(pairs: (String, Int)*) = BSONDocument("$inc" -> BSONDocument(pairs map {
    case (k, v) => k -> BSONInteger(v)
  }))
  def $push[A: Writes](field: String, value: A) = Json.obj("$push" -> Json.obj(field -> value))
  def $pushSlice[A: Writes](field: String, value: A, max: Int) = Json.obj("$push" -> Json.obj(
    field -> Json.obj(
      "$each" -> List(value),
      "$slice" -> max
    )
  ))
  def $pull[A: Writes](field: String, value: A) = Json.obj("$pull" -> Json.obj(field -> value))

  def $gt[A: Writes](value: A) = Json.obj("$gt" -> value)
  def $gte[A: Writes](value: A) = Json.obj("$gte" -> value)
  def $lt[A: Writes](value: A) = Json.obj("$lt" -> value)
  def $lte[A: Writes](value: A) = Json.obj("$lte" -> value)
  def $ne[A: Writes](value: A) = Json.obj("$ne" -> value)

  def $in[A: Writes](values: Iterable[A]) = Json.obj("$in" -> values)
  def $nin[A: Writes](values: Iterable[A]) = Json.obj("$nin" -> values)
  def $all[A: Writes](values: Iterable[A]) = Json.obj("$all" -> values)
  def $exists(bool: Boolean) = Json.obj("$exists" -> bool)

  def $or[A: Writes](conditions: Iterable[A]): JsObject = Json.obj("$or" -> conditions)

  def $regex(value: String, flags: String = "") = BSONFormats toJSON BSONRegex(value, flags)

  import org.joda.time.DateTime
  def $date(value: DateTime) = BSONFormats toJSON BSONDateTime(value.getMillis)

  import reactivemongo.bson.Subtype.GenericBinarySubtype
  def $bin(value: Array[Byte]) = BSONFormats toJSON BSONBinary(value, GenericBinarySubtype)

  private def wrap[K, V: Writes](pairs: Seq[(K, V)]): Seq[(K, Json.JsValueWrapper)] = pairs map {
    case (k, v) => k -> Json.toJsFieldJsValueWrapper(v)
  }
}
