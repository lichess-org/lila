package lila.swiss

import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

import lila.user.User

case class SwissBan(_id: User.ID, date: DateTime, hours: Int)

// final class SwissBanApi()(implicit ec: ExecutionContext) {

//   def currentBan(user: User.ID) =
