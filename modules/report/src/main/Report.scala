package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

case class Report(
    id: String, // also the url slug
    text: String,
    user: String, // the reportee
    processedBy: Option[String],
    createdAt: DateTime,
    createdBy: String) {

  def slug = id

  def isCreator(user: String) = user == createdBy

  def process(by: User) = copy(processedBy = by.id.some)
}

object Report {

  def make(
    user: User,
    text: String,
    createdBy: User): Report = new Report(
    id = Random nextString 8,
    user = user.id,
    text = text,
    processedBy = none,
    createdAt = DateTime.now,
    createdBy = createdBy.id)

  import lila.db.Tube, Tube.Helpers._
  import play.api.libs.json._

  private[report] lazy val tube = Tube(
    (__.json update readDate('createdAt)) andThen Json.reads[Report],
    Json.writes[Report] andThen (__.json update writeDate('createdAt))
  )
}
