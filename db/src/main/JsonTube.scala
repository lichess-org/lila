package lila.db

import play.api.libs.json._
import play.api.libs.functional.syntax._
import Reads.constraints._

case class JsonTube[Doc](
    reads: Reads[Doc],
    writes: Writes[Doc],
    writeTransformer: Option[Reads[JsObject]] = None) {

  lazy val implicits = new {
    implicit val iReads = reads
    implicit val iWrites = writes
  }

  def read(js: JsObject): JsResult[Doc] = reads reads js

  def write(doc: Doc): JsResult[JsObject] = (writes writes doc, writeTransformer) match {
    case (obj: JsObject, Some(transformer)) ⇒ obj transform transformer
    case (obj: JsObject, _)                 ⇒ JsSuccess(obj)
    case (value, _)                         ⇒ JsError()
  }

  def toMongo(doc: Doc): JsResult[JsObject] = 
    write(doc) flatMap JsonTube.toMongo

  def fromMongo(js: JsObject): JsResult[Doc] = 
    depath(JsonTube fromMongo js) flatMap read

  private def depath[A](r: JsResult[A]): JsResult[A] = 
    r.fold(JsError(_), JsSuccess(_))
}

object JsonTube {

  val toMongoTransformer = Helpers.rename('id, '_id)

  val fromMongoTransformer = Helpers.rename('_id, 'id)

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
}
