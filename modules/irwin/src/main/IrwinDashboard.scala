package lila.irwin

import org.joda.time.DateTime

case class IrwinDashboard(recent: List[IrwinReport]) {

  def lastSeenAt = recent.headOption.map(_.date)

  def seenRecently = lastSeenAt.??(DateTime.now.minusMinutes(15).isBefore)
}
