package lila.pref

import play.api.mvc.Request

// because the form structure has changed
// and the mobile app keeps sending the old format
object FormCompatLayer {

  def apply(req: Request[_]): Map[String, Seq[String]] = {
    val data = reqToMap(req).pp
    List(
    "animation",
    "captured",
    "highlight",
    "destination",
    "coords",
    "replay",
    "blindfold").foldLeft(data) {
      case (d, k) => d + (s"display.$k" -> ~d.get(k))
    }.pp
  }

  private def reqToMap(req: Request[_]): Map[String, Seq[String]] = {
    (req.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined => body.asMultipartFormData.get.asFormUrlEncoded
      case _ => Map.empty[String, Seq[String]]
    }) ++ req.queryString
  }
}
