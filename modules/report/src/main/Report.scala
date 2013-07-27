package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Report(
    id: String, // also the url slug
    text: String,
    processedBy: Option[String],
    createdAt: DateTime,
    createdBy: String) {

  def slug = id

  def isCreator(user: String) = user == createdBy
}

object Report {

  def make(
    text: String,
    createdBy: String): Report = new Report(
    id = Random nextString 8,
    text = text,
    processedBy = none,
    createdAt = DateTime.now,
    createdBy = createdBy)

  import lila.db.Tube, Tube.Helpers._
  import play.api.libs.json._

  private[report] lazy val tube = Tube(
    (__.json update readDate('createdAt)) andThen Json.reads[Report],
    Json.writes[Report] andThen (__.json update writeDate('createdAt))
  )
}
