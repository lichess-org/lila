package lidraughts.activity

import org.joda.time.Interval

import lidraughts.game.Pov
import lidraughts.practice.PracticeStudy
import lidraughts.simul.Simul
import lidraughts.study.Study
import lidraughts.tournament.LeaderboardApi.{ Entry => TourEntry }

import activities._
import model._

case class ActivityView(
    interval: Interval,
    games: Option[Games] = None,
    puzzles: Option[Puzzles] = None,
    practice: Option[Map[PracticeStudy, Int]] = None,
    simuls: Option[List[Simul]] = None,
    patron: Option[Patron] = None,
    posts: Option[Map[lidraughts.forum.Topic, List[lidraughts.forum.Post]]] = None,
    corresMoves: Option[(Int, List[Pov])] = None,
    corresEnds: Option[(Score, List[Pov])] = None,
    follows: Option[Follows] = None,
    studies: Option[List[Study.IdName]] = None,
    teams: Option[Teams] = None,
    tours: Option[ActivityView.Tours] = None,
    stream: Boolean = false,
    signup: Boolean = false
)

object ActivityView {

  case class Tours(
      nb: Int,
      best: List[TourEntry]
  )
}
