package lila.activity

import activities.*
import model.*

import lila.game.LightPov
import lila.practice.PracticeStudy
import lila.simul.Simul
import lila.study.Study
import lila.swiss.Swiss
import lila.tournament.LeaderboardApi.{ Entry as TourEntry }
import lila.ublog.UblogPost

case class ActivityView(
    interval: TimeInterval,
    games: Option[Games] = None,
    puzzles: Option[Puzzles] = None,
    storm: Option[Storm] = None,
    racer: Option[Racer] = None,
    streak: Option[Streak] = None,
    practice: Option[Map[PracticeStudy, Int]] = None,
    simuls: Option[List[Simul]] = None,
    patron: Option[Patron] = None,
    forumPosts: Option[Map[lila.forum.ForumTopic, List[lila.forum.ForumPost]]] = None,
    ublogPosts: Option[List[UblogPost.LightPost]] = None,
    corresMoves: Option[(Int, List[LightPov])] = None,
    corresEnds: Option[(Score, List[LightPov])] = None,
    follows: Option[Follows] = None,
    studies: Option[List[Study.IdName]] = None,
    teams: Option[Teams] = None,
    tours: Option[ActivityView.Tours] = None,
    swisses: Option[List[(Swiss.IdName, Rank)]] = None,
    stream: Boolean = false,
    signup: Boolean = false
):
  def isEmpty = !stream && List(
    games,
    puzzles,
    storm,
    racer,
    streak,
    practice,
    simuls,
    patron,
    forumPosts,
    ublogPosts,
    corresMoves,
    corresEnds,
    follows,
    studies,
    teams,
    tours,
    swisses
  ).forall(_.isEmpty)

object ActivityView:

  case class Tours(
      nb: Int,
      best: List[TourEntry]
  )
