package lila.activity

import org.joda.time.DateTime

import lila.study.Study
import lila.practice.PracticeStudy
import lila.game.Pov
import lila.simul.Simul
import lila.study.Study
import lila.tournament.LeaderboardApi.{ Entry => TourEntry }

import activities._
import model._

case class ActivityView(
    games: Option[Games],
    puzzles: Option[Puzzles],
    practice: Option[Map[PracticeStudy, Int]],
    simuls: Option[List[Simul]],
    patron: Option[Patron],
    posts: Option[Map[lila.forum.Topic, List[lila.forum.Post]]],
    corresMoves: Option[(Int, List[Pov])],
    corresEnds: Option[(Score, List[Pov])],
    follows: Option[Follows],
    studies: Option[List[Study.IdName]],
    tours: Option[ActivityView.Tours]
) {
}

object ActivityView {

  case class Tours(
    nb: Int,
    best: List[TourEntry]
  )

  case class AsTo(day: DateTime, activity: ActivityView)
}
