package lila.db

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.utils.Converters
import play.api.libs.json._
import play.modules.reactivemongo._

// JsObject elements are not suitable for pattern matching.
// the Seq has to be transformed into a List
object PlayReactiveMongoPatch {

  implicit object JsObjectWriter extends BSONDocumentWriter[JsObject] {
    private val specials: PartialFunction[JsValue, BSONValue] = {
      case obj: JsObject if obj.value.headOption.exists(s ⇒ s._2.isInstanceOf[JsString] && s._1 == "$oid") ⇒
        BSONObjectID(obj.value.head._2.as[String])
      case JsObject(("$oid", JsString(v)) :: Nil)    ⇒ BSONObjectID(Converters.str2Hex(v))
      case JsObject(("$date", JsNumber(v)) :: _)   ⇒ BSONDateTime(v.toLong)
      case JsObject(("$int", JsNumber(v)) :: _)    ⇒ BSONInteger(v.toInt)
      case JsObject(("$long", JsNumber(v)) :: _)   ⇒ BSONLong(v.toLong)
      case JsObject(("$double", JsNumber(v)) :: _) ⇒ BSONDouble(v.toDouble)
    }

    private def matchable(obj: JsValue): JsValue = obj match {
      case JsObject(fields) => JsObject(fields.toList)
      case other => other
    }

    def write(obj: JsObject): BSONDocument = {
      BSONDocument(obj.fields.map { tuple ⇒
        tuple._1 -> specials.lift(matchable(tuple._2)).getOrElse(toBSON(tuple._2))
      }.toStream)
    }
  }

  def toBSON(value: JsValue): BSONValue = value match {
    case s: JsString => BSONString(s.value)
    case i: JsNumber => BSONDouble(i.value.toDouble)
    case o: JsObject => JsObjectWriter.write(o)
    case a: JsArray =>
      BSONArray(a.value.map { elem =>
        toBSON(elem)
      }.toStream)
    case b: JsBoolean   => BSONBoolean(b.value)
    case JsNull         => BSONNull
    case u: JsUndefined => BSONUndefined
  }
}
