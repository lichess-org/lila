package lila.activity

import play.api.i18n.Lang
import scalalib.HeapSort

import lila.core.chess.Rank
import lila.core.game.LightPov
import lila.core.swiss.IdName as SwissIdName
import lila.db.AsyncCollFailingSilently
import lila.db.dsl.*
import chess.Speed.Correspondence

final class ActivityReadApi(
    coll: AsyncCollFailingSilently,
    gameRepo: lila.core.game.GameRepo,
    getPracticeStudies: lila.core.practice.GetStudies,
    forumPostApi: lila.core.forum.ForumPostApi,
    ublogApi: lila.core.ublog.UblogApi,
    simulApi: lila.core.simul.SimulApi,
    studyApi: lila.core.study.StudyApi,
    tourLeaderApi: lila.core.tournament.leaderboard.Api,
    swissApi: lila.core.swiss.SwissApi,
    teamApi: lila.core.team.TeamApi,
    lightUserApi: lila.core.user.LightUserApi,
    getTourName: lila.core.tournament.GetTourName
)(using Executor):

  import BSONHandlers.{ *, given }

  private given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  def recentAndPreload(u: User)(using lang: Lang): Fu[List[ActivityView]] = for
    activities <-
      coll(
        _.find(regexId(u.id))
          .sort($sort.desc("_id"))
          .cursor[Activity]()
          .list(Activity.recentNb)
      ).dmap(_.filterNot(_.isEmpty))
        .mon(_.user.segment("activity.raws"))
    practiceStudies <- activities
      .exists(_.practice.isDefined)
      .soFu(getPracticeStudies())
    views <- activities.sequentially: a =>
      one(practiceStudies, a).mon(_.user.segment("activity.view"))
    _ <- preloadAll(views)
  yield addSignup(u.createdAt, views)

  private def preloadAll(views: Seq[ActivityView])(using lang: Lang) = for
    _ <- lightUserApi.preloadMany(views.flatMap(_.follows.so(_.allUserIds)))
    _ <- getTourName.preload(views.flatMap(_.tours.so(_.best.map(_.tourId))))
  yield ()

  private def one(practiceStudies: Option[lila.core.practice.Studies], a: Activity): Fu[ActivityView] =
    for
      allForumPosts <- a.forumPosts.soFu: p =>
        forumPostApi
          .miniViews(p.value)
          .mon(_.user.segment("activity.posts"))
      hiddenForumTeamIds <- teamApi.filterHideForum(
        (~allForumPosts).flatMap(_.topic.possibleTeamId).distinct
      )
      forumPosts = allForumPosts.map(
        _.filterNot(_.topic.possibleTeamId.exists(hiddenForumTeamIds.contains))
      )
      ublogPosts <- a.ublogPosts
        .soFu: p =>
          ublogApi
            .liveLightsByIds(p.value)
            .mon(_.user.segment("activity.ublogs"))
        .dmap(_.filter(_.nonEmpty))
      practice = for
        p <- a.practice
        studies <- practiceStudies
      yield p.value.flatMap { (studyId, nb) =>
        studies(studyId).map(_ -> nb)
      }.toMap
      forumPostView = forumPosts
        .map: p =>
          p.groupBy(_.topic)
            .view
            .mapValues: posts =>
              posts.view.map(_.post).sortBy(_.createdAt).toList
            .toMap
        .filter(_.nonEmpty)
      corresMoves <- a.corres.so: corres =>
        getLightPovs(a.id.userId, corres.movesIn).dmap:
          _.map(corres.moves -> _)
      corresEnds <- a.corres.so: corres =>
        getLightPovs(a.id.userId, corres.end).dmap:
          _.map:
            _.groupBy(pov => PerfKey(pov.game.variant, Correspondence)).view
              .mapValues: groupedPovs =>
                (Score.make(groupedPovs) -> groupedPovs)
              .toMap
      simuls <- a.simuls
        .soFu: simuls =>
          simulApi.byIds(simuls.value)
        .dmap(_.filter(_.nonEmpty))
      studies <- a.studies
        .soFu: studies =>
          studyApi.publicIdNames(studies.value)
        .dmap(_.filter(_.nonEmpty))
      tours <- a.games
        .exists(_.hasNonCorres)
        .so:
          val dateRange = TimeInterval(a.date, a.date.plusDays(1))
          tourLeaderApi
            .timeRange(a.id.userId, dateRange)
            .dmap: entries =>
              entries.nonEmpty.option(
                ActivityView.Tours(
                  nb = entries.size,
                  best = HeapSort.topN(entries, activities.maxSubEntries)(using
                    Ordering.by[lila.core.tournament.leaderboard.Entry, Double](-_.rankRatio.value)
                  )
                )
              )
            .mon(_.user.segment("activity.tours"))
      swisses <-
        a.swisses.so: swisses =>
          toSwissesView(swisses.value).dmap(_.some.filter(_.nonEmpty))
    yield ActivityView(
      interval = a.interval,
      games = a.games,
      puzzles = a.puzzles,
      storm = a.storm,
      racer = a.racer,
      streak = a.streak,
      practice = practice,
      forumPosts = forumPostView,
      ublogPosts = ublogPosts,
      simuls = simuls,
      patron = a.patron,
      corresMoves = corresMoves,
      corresEnds = corresEnds,
      follows = a.follows,
      studies = studies,
      teams = a.teams,
      tours = tours,
      swisses = swisses,
      stream = a.stream
    )

  def recentSwissRanks(userId: UserId): Fu[List[(SwissIdName, Rank)]] =
    coll(
      _.find(regexId(userId) ++ $doc(BSONHandlers.ActivityFields.swisses.$exists(true)))
        .sort($sort.desc("_id"))
        .cursor[Activity]()
        .list(10)
    ).flatMap { activities =>
      toSwissesView(activities.flatMap(_.swisses.so(_.value)))
    }

  private def toSwissesView(swisses: List[activities.SwissRank]): Fu[List[(SwissIdName, Rank)]] =
    swissApi
      .idNames(swisses.map(_.id))
      .map:
        _.flatMap: idName =>
          swisses
            .find(_.id == idName.id)
            .map: s =>
              (idName, s.rank)

  private def addSignup(at: Instant, recent: List[ActivityView]) =
    val (found, views) = recent.foldLeft(false -> List.empty[ActivityView]):
      case ((false, as), a) if a.interval.contains(at) => (true, as :+ a.copy(signup = true))
      case ((found, as), a) => (found, as :+ a)
    if !found && views.sizeIs < Activity.recentNb && nowInstant.minusDays(8).isBefore(at) then
      views :+ ActivityView(
        interval = TimeInterval(at.withTimeAtStartOfDay, at.withTimeAtStartOfDay.plusDays(1)),
        signup = true
      )
    else views

  private def getLightPovs(userId: UserId, gameIds: List[GameId]): Fu[Option[List[LightPov]]] =
    gameIds.nonEmpty.so:
      gameRepo.light
        .gamesFromSecondary(gameIds)
        .dmap:
          _.flatMap { LightPov(_, userId) }.some.filter(_.nonEmpty)
