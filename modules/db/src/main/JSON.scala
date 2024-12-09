package lila.db

import play.api.libs.json.*
import reactivemongo.api.bson.*

object JSON:

  def bdoc(json: JsObject): BSONDocument =
    BSONDocument(json.fields.map { (k, v) => k -> bval(v) })

  def bval(json: JsValue): BSONValue = json match
    case JsString(value)  => BSONString(value)
    case JsNumber(value)  => BSONDouble(value.toDouble)
    case JsBoolean(value) => BSONBoolean(value)
    case obj: JsObject    => bdoc(obj)
    case JsArray(arr)     => BSONArray(arr.map(bval))
    case _                => BSONNull

  def jval(bson: BSONDocument): JsValue = Json.obj(
    bson.elements.map(el => el.name -> jval(el.value))*
  )

  def jval(bson: BSONValue): JsValue = bson match
    case BSONString(value)  => JsString(value)
    case BSONDouble(value)  => JsNumber(value)
    case BSONInteger(value) => JsNumber(value)
    case BSONBoolean(value) => JsBoolean(value)
    case obj: BSONDocument  => jval(obj)
    case BSONArray(values)  => JsArray(values.map(jval))
    case v                  => JsString(v.toString)
