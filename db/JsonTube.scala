package lila.db

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class JsonTube[Doc](reads: Reads[Doc], writes: Writes[Doc]) {

  lazy val implicits = new {
    implicit val iReads = reads
    implicit val iWrites = writes
  }

  def read(js: JsObject): JsResult[Doc] = reads reads js

  def write(doc: Doc): JsValue = writes writes doc 

  def toMongo(doc: Doc): JsResult[JsValue] = JsonTube toMongo write(doc) 

  def fromMongo(js: JsObject): JsResult[Doc] = (JsonTube fromMongo js).pp flatMap read
}

object JsonTube {

  val toMongoTransformer = rename('id, '_id)

  val fromMongoTransformer = rename('_id, 'id)

  private def rename(from: Symbol, to: Symbol) = __.json update ( 
    (__ \ to).json copyFrom (__ \ from).json.pick 
  ) andThen (__ \ from).json.prune 


  def toMongo(js: JsValue): JsResult[JsObject] = js transform toMongoTransformer

  def fromMongo(js: JsValue): JsResult[JsObject] = js transform fromMongoTransformer
}
