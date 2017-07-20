package lila.activity

import org.joda.time.DateTime

import lila.study.Study
import lila.practice.PracticeStudy
import lila.game.Pov

import activities._
import model._

case class ActivityView(
    games: Option[Games],
    puzzles: Option[Puzzles],
    practice: Option[Map[PracticeStudy, Int]],
    patron: Option[Patron],
    posts: Option[Map[lila.forum.Topic, List[lila.forum.Post]]],
    corresMoves: Option[(Int, List[Pov])],
    corresEnds: Option[(Score, List[Pov])]
) {
}

object ActivityView {

  case class AsTo(day: DateTime, activity: ActivityView)
}
