package lila.irwin

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._

final class IrwinApi(
    reportColl: Coll
) {

  import BSONHandlers._

  def insert(report: IrwinReport) = reportColl.update($id(report.id), report, upsert = true)
}
