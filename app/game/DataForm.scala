package lila
package game

import play.api.data._
import play.api.data.Forms._

final class DataForm {

  val importForm = Form(mapping(
    "pgn" -> nonEmptyText
  )(ImportData.apply)(ImportData.unapply)) 
}

case class ImportData(pgn: String) {
}
