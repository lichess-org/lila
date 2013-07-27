package lila.report

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.api._

import lila.db.api._
import tube.reportTube

object ReportRepo {

  type ID = String
}
