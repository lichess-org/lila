package lidraughts.socket

import draughts.opening._
import draughts.variant.Variant
import play.api.libs.json._

object GetOpening {

  import lidraughts.tree.Node.openingWriter

  def apply(o: JsObject): Option[JsObject] = for {
    d <- o obj "d"
    variant = draughts.variant.Variant orDefault ~d.str("variant")
    fen <- d str "fen"
    path <- d str "path"
    opening <- Variant.openingSensibleVariants(variant) ?? {
      FullOpeningDB findByFen fen
    }
  } yield Json.obj(
    "path" -> path,
    "opening" -> opening
  )
}
