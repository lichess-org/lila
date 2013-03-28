package lila.db

import Types.Coll

import play.api.libs.json._
import play.api.libs.functional.syntax._
import Reads.constraints._
import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._

case class Tube[Doc](
  reader: Reads[Doc],
  writer: Writes[Doc],
  writeTransformer: Option[Reads[JsObject]] = None)
    extends Reads[Doc]
    with Writes[Doc]
    with BSONDocumentReader[Option[Doc]] {

  implicit def reads(js: JsValue): JsResult[Doc] = reader reads js
  implicit def writes(doc: Doc): JsValue = writer writes doc

  def read(bson: BSONDocument): Option[Doc] =
    fromMongo(JsObjectReader read bson).asOpt

  def read(js: JsObject): JsResult[Doc] = reads(js)

  def write(doc: Doc): JsResult[JsObject] = (writes(doc), writeTransformer) match {
    case (obj: JsObject, Some(transformer)) ⇒ obj transform transformer
    case (obj: JsObject, _)                 ⇒ JsSuccess(obj)
    case (value, _)                         ⇒ JsError()
  }

  def toMongo(doc: Doc): JsResult[JsObject] =
    write(doc) flatMap Tube.toMongo

  def fromMongo(js: JsObject): JsResult[Doc] =
    Tube.depath(Tube fromMongo js) flatMap read
}

object Tube {

  private val toMongoTransformer = Helpers.rename('id, '_id)

  private val fromMongoTransformer = Helpers.rename('_id, 'id)

  def toMongo(js: JsValue): JsResult[JsObject] = js transform toMongoTransformer

  def fromMongo(js: JsValue): JsResult[JsObject] = js transform fromMongoTransformer

  object Helpers {

    def rename(from: Symbol, to: Symbol) = __.json update (
      (__ \ to).json copyFrom (__ \ from).json.pick
    ) andThen (__ \ from).json.prune

    def readDate(field: Symbol) =
      (__ \ field).json.update(of[JsObject] map (_ \ "$date"))

    def writeDate(field: Symbol) = (__ \ field).json.update(of[JsNumber] map {
      millis ⇒ Json.obj("$date" -> millis)
    })

    def merge(obj: JsObject) = __.read[JsObject] map (obj ++)
  }

  private def depath[A](r: JsResult[A]): JsResult[A] =
    r.fold(JsError(_), JsSuccess(_))
}
