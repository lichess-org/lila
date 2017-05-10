package lila.irwin

import org.joda.time.DateTime

case class IrwinDashboard(
    queue: List[IrwinRequest],
    recent: List[IrwinReport]
) {

  def lastSeenAt = recent.headOption.map(_.date)
}
