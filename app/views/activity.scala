package views.html

import controllers.routes

import lila.activity.activities.*
import lila.activity.model.*

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

object activity:

  def apply(u: User.WithPerfs, as: Iterable[lila.activity.ActivityView])(using Context) =
    div(cls := "activity")(
      as.toSeq filterNot (_.isEmpty) map { a =>
        st.section(
          h2(semanticDate(a.interval.start)),
          div(cls := "entries")(
            a.patron map renderPatron,
            a.practice map renderPractice,
            a.puzzles map renderPuzzles(u),
            a.storm map renderStorm,
            a.racer map renderRacer,
            a.streak map renderStreak,
            a.games map renderGames,
            a.forumPosts map renderForumPosts,
            a.ublogPosts map renderUblogPosts(u.user),
            a.corresMoves map renderCorresMoves,
            a.corresEnds map renderCorresEnds,
            a.follows map renderFollows,
            a.simuls map renderSimuls(u.user),
            a.studies map renderStudies,
            a.tours map renderTours,
            a.swisses map renderSwisses,
            a.teams map renderTeams,
            a.stream option renderStream(u.user),
            a.signup option renderSignup
          )
        )
      }
    )

  private def subCount(count: Int) = if count >= maxSubEntries then s"$count+" else s"$count"

  private def renderPatron(p: Patron)(using Context) =
    div(cls := "entry plan")(
      iconTag(licon.Wings),
      div(
        if p.months == 0 then a(href := routes.Plan.index)("Lifetime Patron!")
        else
          trans.activity.supportedNbMonths.plural(p.months, p.months, a(href := routes.Plan.index)("Patron"))
      )
    )

  private def renderPractice(p: Map[lila.practice.PracticeStudy, Int])(using Context) =
    val ps = p.toSeq.sortBy(-_._2)
    entryTag(
      iconTag(licon.Bullseye),
      div(
        ps.headOption map onePractice,
        ps match
          case _ :: rest if rest.nonEmpty => subTag(rest map onePractice)
          case _                          => emptyFrag
      )
    )

  private def onePractice(tup: (lila.practice.PracticeStudy, Int))(using Context) =
    val (study, nb) = tup
    val href        = routes.Practice.show("-", study.slug, study.id)
    frag(
      trans.activity.practicedNbPositions.plural(nb, nb, a(st.href := href)(study.name)),
      br
    )

  private def renderPuzzles(u: User.WithPerfs)(p: Puzzles)(using ctx: Context) =
    entryTag(
      iconTag(licon.ArcheryTarget),
      scoreFrag(p.value),
      div(
        trans.activity.solvedNbPuzzles.pluralSame(p.value.size),
        p.value.rp.filterNot(_.isEmpty || (u.perfs.dubiousPuzzle && !ctx.is(u))).map(ratingProgFrag)
      )
    )

  private def renderStorm(s: Storm)(using Context) =
    entryTag(
      iconTag(licon.Storm),
      scoreTag(winTag(trans.storm.highscoreX(strong(s.score)))),
      div(
        trans.storm.playedNbRunsOfPuzzleStorm
          .plural(s.runs, s.runs.localize, a(href := routes.Storm.home)("Puzzle Storm"))
      )
    )

  private def renderRacer(s: Racer)(using Context) =
    entryTag(
      iconTag(licon.FlagChessboard),
      scoreTag(winTag(trans.storm.highscoreX(strong(s.score)))),
      div(
        trans.storm.playedNbRunsOfPuzzleStorm
          .plural(s.runs, s.runs.localize, a(href := routes.Racer.home)("Puzzle Racer"))
      )
    )

  private def renderStreak(s: Streak)(using Context) =
    entryTag(
      iconTag(licon.ArrowThruApple),
      scoreTag(winTag(trans.storm.highscoreX(strong(s.score)))),
      div(
        trans.storm.playedNbRunsOfPuzzleStorm
          .plural(s.runs, s.runs.localize, a(href := routes.Puzzle.streak)("Puzzle Streak"))
      )
    )

  private def renderGames(games: Games)(using Context) =
    games.value.toSeq.sortBy(-_._2.size).map { case (pt, score) =>
      entryTag(
        iconTag(pt.icon),
        scoreFrag(score),
        div(
          trans.activity.playedNbGames.plural(score.size, score.size, pt.trans),
          score.rp.filterNot(_.isEmpty).map(ratingProgFrag)
        )
      )
    }

  private def renderForumPosts(posts: Map[lila.forum.ForumTopic, List[lila.forum.ForumPost]])(using
      ctx: Context
  ) =
    ctx.kid.no option entryTag(
      iconTag(licon.BubbleConvo),
      div(
        posts.toSeq.map: (topic, posts) =>
          frag(
            trans.activity.postedNbMessages
              .plural(
                posts.size,
                posts.size,
                a(href := routes.ForumTopic.show(topic.categId, topic.slug))(shorten(topic.name, 70))
              ),
            subTag(
              posts.map { post =>
                div(cls := "line")(a(href := routes.ForumPost.redirect(post.id))(shorten(post.text, 120)))
              }
            )
          )
      )
    )

  private def renderUblogPosts(user: User)(posts: List[lila.ublog.UblogPost.LightPost])(using
      ctx: Context
  ) =
    ctx.kid.no option entryTag(
      iconTag(licon.InkQuill),
      div(
        trans.ublog.publishedNbBlogPosts.pluralSame(posts.size),
        subTag(posts.map: post =>
          div:
            a(href := routes.Ublog.post(user.username, post.slug, post.id))(shorten(post.title, 120))
        )
      )
    )

  private def renderCorresMoves(nb: Int, povs: List[lila.game.LightPov])(using Context) =
    entryTag(
      iconTag(licon.PaperAirplane),
      div(
        trans.activity.playedNbMoves.pluralSame(nb),
        " ",
        trans.activity.inNbCorrespondenceGames.plural(povs.size, subCount(povs.size)),
        subTag(
          povs.map: pov =>
            frag(
              a(cls := "glpt", href := routes.Round.watcher(pov.gameId, pov.color.name))("Game"),
              " vs ",
              lightPlayerLink(
                pov.opponent,
                withRating = true,
                withDiff = false,
                withOnline = true,
                link = true
              ),
              br
            )
        )
      )
    )

  private def renderCorresEnds(score: Score, povs: List[lila.game.LightPov])(using Context) =
    entryTag(
      iconTag(licon.PaperAirplane),
      div(
        trans.activity.completedNbGames.plural(score.size, subCount(score.size)),
        score.rp.filterNot(_.isEmpty).map(ratingProgFrag),
        scoreFrag(score),
        subTag(
          povs.map: pov =>
            frag(
              a(cls := "glpt", href := routes.Round.watcher(pov.gameId, pov.color.name))(
                pov.game.win.map(_ == pov.color) match
                  case Some(true)  => trans.victory()
                  case Some(false) => trans.defeat()
                  case _           => "Draw"
              ),
              " vs ",
              lightPlayerLink(
                pov.opponent,
                withRating = true,
                withDiff = false,
                withOnline = true,
                link = true
              ),
              br
            )
        )
      )
    )

  private def renderFollows(all: Follows)(using Context) =
    entryTag(
      iconTag(licon.ThumbsUp),
      div(
        List(all.in.map(_ -> true), all.out.map(_ -> false)).flatten map { (f, in) =>
          frag(
            if in then trans.activity.gainedNbFollowers.pluralSame(f.actualNb)
            else trans.activity.followedNbPlayers.pluralSame(f.actualNb),
            subTag(
              fragList(f.ids.map(id => userIdLink(id.some))),
              f.nb.map { nb =>
                frag(" and ", nb - maxSubEntries, " more")
              }
            )
          )
        }
      )
    )

  private def renderSimuls(u: User)(simuls: List[lila.simul.Simul])(using Context) =
    entryTag(
      iconTag(licon.Group),
      div(
        simuls.groupBy(_.isHost(u.some)).toSeq.map { (isHost, simuls) =>
          frag(
            if isHost then trans.activity.hostedNbSimuls.pluralSame(simuls.size)
            else trans.activity.joinedNbSimuls.pluralSame(simuls.size),
            subTag(
              simuls.map: s =>
                val win = s.pairingOf(u.id).flatMap(_.wins)
                div(
                  a(href := routes.Simul.show(s.id))(
                    s.name,
                    " simul by ",
                    userIdLink(s.hostId.some)
                  ),
                  if isHost then scoreFrag(Score(s.wins, s.losses, s.draws, none))
                  else scoreFrag(Score(win.has(true) so 1, win.has(false) so 1, win.isEmpty so 1, none))
                )
            )
          )
        }
      )
    )

  private def renderStudies(studies: List[lila.study.Study.IdName])(using Context) =
    entryTag(
      iconTag(licon.StudyBoard),
      div(
        trans.activity.createdNbStudies.pluralSame(studies.size),
        subTag:
          studies.map: s =>
            frag(a(href := routes.Study.show(s.id))(s.name), br)
      )
    )

  private def renderTeams(teams: Teams)(using ctx: Context) =
    ctx.kid.no option entryTag(
      iconTag(licon.Group),
      div(
        trans.activity.joinedNbTeams.pluralSame(teams.value.size),
        subTag(fragList(teams.value.map(id => teamLink(id))))
      )
    )

  private def renderTours(tours: lila.activity.ActivityView.Tours)(using Context) =
    entryTag(
      iconTag(licon.Trophy),
      div(
        trans.activity.competedInNbTournaments.pluralSame(tours.nb),
        subTag:
          tours.best.map: t =>
            div(
              cls := List(
                "is-gold" -> (t.rank == Rank(1)),
                "text"    -> (t.rank <= 3)
              ),
              dataIcon := (t.rank <= 3).option(licon.Trophy)
            )(
              trans.activity.rankedInTournament.plural(
                t.nbGames,
                strong(t.rank),
                t.rankRatio.percent,
                t.nbGames,
                a(href := routes.Tournament.show(t.tourId))(tournamentIdToName(t.tourId))
              ),
              br
            )
      )
    )

  private def renderSwisses(swisses: List[(lila.swiss.Swiss.IdName, Rank)])(using Context) =
    entryTag(
      iconTag(licon.Trophy),
      div(
        trans.activity.competedInNbSwissTournaments.pluralSame(swisses.size),
        subTag:
          swisses.map: (swiss, rank) =>
            div(
              cls := List(
                "is-gold" -> (rank == Rank(1)),
                "text"    -> (rank <= 3)
              ),
              dataIcon := (rank <= 3).option(licon.Trophy)
            )(
              trans.activity.rankedInSwissTournament(
                strong(rank),
                a(href := routes.Swiss.show(swiss.id))(swiss.name)
              ),
              br
            )
      )
    )

  private def renderStream(u: User)(using ctx: Context) =
    ctx.kid.no option entryTag(
      iconTag(licon.Mic),
      a(href := routes.Streamer.redirect(u.username))(trans.activity.hostedALiveStream())
    )

  private def renderSignup(using Context) =
    entryTag(
      iconTag(licon.StarOutline),
      div(trans.activity.signedUp())
    )

  private val entryTag = div(cls := "entry")
  private val subTag   = div(cls := "sub")
  private val scoreTag = tag("score")
  private val winTag   = tag("win")

  private def scoreFrag(s: Score)(using Context) = raw:
    s"""<score>${scoreStr("win", s.win, trans.nbWins)}${scoreStr("draw", s.draw, trans.nbDraws)}${scoreStr(
        "loss",
        s.loss,
        trans.nbLosses
      )}</score>"""

  private def ratingProgFrag(r: RatingProg)(using ctx: Context) =
    ctx.pref.showRatings option ratingTag(r.after.value, ratingProgress(r.diff))

  private def scoreStr(tag: String, p: Int, name: lila.i18n.I18nKey)(using Context) =
    if p == 0 then ""
    else s"""<$tag>${wrapNumber(name.pluralSameTxt(p))}</$tag>"""

  private val wrapNumberRegex         = """(\d++)""".r
  private def wrapNumber(str: String) = wrapNumberRegex.replaceAllIn(str, "<strong>$1</strong>")
