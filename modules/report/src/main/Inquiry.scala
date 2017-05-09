package lila.report

import org.joda.time.DateTime

import lila.common.LightUser
import lila.user.User

case class Inquiry(mod: User.ID, seenAt: DateTime)
