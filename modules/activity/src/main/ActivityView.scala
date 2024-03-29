package lila.activity

import lila.game.LightPov
import lila.simul.Simul
import lila.hub.swiss.{ IdName as SwissIdName }
import lila.hub.ublog.UblogPost
import lila.activity.activities.*
import lila.activity.model.*
import lila.hub.forum.ForumPostMini
import lila.hub.forum.ForumTopicMini

case class ActivityView(
    interval: TimeInterval,
    games: Option[Games] = None,
    puzzles: Option[Puzzles] = None,
    storm: Option[Storm] = None,
    racer: Option[Racer] = None,
    streak: Option[Streak] = None,
    practice: Option[Map[lila.hub.practice.Study, Int]] = None,
    simuls: Option[List[Simul]] = None,
    patron: Option[Patron] = None,
    forumPosts: Option[Map[ForumTopicMini, List[ForumPostMini]]] = None,
    ublogPosts: Option[List[UblogPost.LightPost]] = None,
    corresMoves: Option[(Int, List[LightPov])] = None,
    corresEnds: Option[(Score, List[LightPov])] = None,
    follows: Option[Follows] = None,
    studies: Option[List[lila.hub.study.IdName]] = None,
    teams: Option[Teams] = None,
    tours: Option[ActivityView.Tours] = None,
    swisses: Option[List[(SwissIdName, Rank)]] = None,
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

  case class Tours(nb: Int, best: List[lila.hub.tournament.leaderboard.Entry])
