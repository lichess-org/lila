package lila.activity

import org.joda.time.DateTime

import lila.user.User

import activities._

case class ActivityView(
    games: Option[Games],
    puzzles: Option[Puzzles]
) {
}

object ActivityView {

  case class AsTo(day: DateTime, activity: ActivityView)
}
