package lila
package mod

import play.api.libs.json._

case class JsonTube[Doc](
    reads: Reads[Doc],
    writes: Writes[Doc]) {

  def read(js: JsObject): JsResult[Doc] = reads reads js

  def unsafeRead(js: JsObject): Doc = read(js) recover { err ⇒
    throw new Exception(Json.stringify(JsError toFlatJson err))
  }

  def write(doc: Doc): JsValue = writes writes doc
}
