package lila.gathering

import lila.common.Bus
import lila.core.msg.PayoutMessages

def payoutDms(userApi: lila.core.user.UserApi, users: List[String])(
    payout: List[UserId] => PayoutMessages
)(using Executor): Fu[String] =
  val userIds = users.map(UserStr(_).id).distinct
  userApi
    .filterExists(userIds.toSet)
    .map: found =>
      val missing = userIds.filterNot(found.contains)
      if missing.nonEmpty then s"Unknown users: ${missing.mkString(", ")}. Nothing sent."
      else
        val msg = payout(userIds)
        Bus.pub(msg)
        s"Sent payout DM for ${msg.tourName} to ${userIds.mkString(", ")}"
