package lila.db

import play.api.libs.json._

case class JsonTube[Doc](
    reads: Reads[Doc],
    writes: Writes[Doc]) {

  lazy val implicits = new {
    implicit val iReads = reads
    implicit val iWrites = writes
  }

  def read(js: JsObject): JsResult[Doc] = reads reads js

  def unsafeRead(js: JsObject): Doc = read(js) recoverTotal { err ⇒
    throw new Exception(Json.stringify(JsError toFlatJson err))
  }

  def write(doc: Doc): JsValue = writes writes doc
}
