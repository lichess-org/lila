package lila.pref

import play.api.mvc.Request

// because the form structure has changed
// and the mobile app keeps sending the old format
object FormCompatLayer {

  private type FormData = Map[String, Seq[String]]

  def apply(pref: Pref, req: Request[_]): FormData =
    reqToFormData(req) |>
      addMissing("moretime", pref.moretime.toString) |>
      moveTo("behavior", List(
        "moveEvent",
        "premove",
        "takeback",
        "autoQueen",
        "autoThreefold",
        "submitMove",
        "confirmResign",
        "moretime",
        "keyboardMove"
      )) |>
      moveTo("display", List(
        "animation",
        "captured",
        "highlight",
        "destination",
        "coords",
        "replay",
        "pieceNotation",
        "blindfold"
      ))

  private def addMissing(path: String, default: String)(data: FormData): FormData =
    data.updated(path, data.getOrElse(path, List(default)))

  private def moveTo(prefix: String, fields: List[String])(data: FormData): FormData =
    fields.foldLeft(data) {
      case (d, field) =>
        val newField = s"$prefix.$field"
        d + (newField -> ~d.get(newField).orElse(d.get(field)))
    }

  private def reqToFormData(req: Request[_]): FormData = {
    (req.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined => body.asMultipartFormData.get.asFormUrlEncoded
      case _ => Map.empty[String, Seq[String]]
    }) ++ req.queryString
  }
}
