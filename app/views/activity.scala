package views.html

import controllers.routes

import lila.activity.activities._
import lila.activity.model._
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.swiss.Swiss

object activity {

  def apply(u: User, as: Iterable[lila.activity.ActivityView])(implicit ctx: Context) =
    div(cls := "activity")(
      as.toSeq map { a =>
        st.section(
          h2(semanticDate(a.interval.getStart)),
          div(cls := "entries")(
            a.patron map renderPatron,
            a.practice map renderPractice,
            a.puzzles map renderPuzzles(u),
            a.storm map renderStorm,
            a.racer map renderRacer,
            a.streak map renderStreak,
            a.games map renderGames,
            a.posts map renderPosts,
            a.corresMoves map { case (nb, povs) =>
              renderCorresMoves(nb, povs)
            },
            a.corresEnds map { case (score, povs) =>
              renderCorresEnds(score, povs)
            },
            a.follows map renderFollows,
            a.simuls map renderSimuls(u),
            a.studies map renderStudies,
            a.tours map renderTours,
            a.swisses map renderSwisses,
            a.teams map renderTeams,
            a.stream option renderStream(u),
            a.signup option renderSignup
          )
        )
      }
    )

  private def subCount(count: Int) = if (count >= maxSubEntries) s"$count+" else s"$count"

  private def renderPatron(p: Patron)(implicit ctx: Context) =
    div(cls := "entry plan")(
      iconTag(""),
      div(
        trans.activity.supportedNbMonths.plural(p.months, p.months, a(href := routes.Plan.index)("Patron"))
      )
    )

  private def renderPractice(p: Map[lila.practice.PracticeStudy, Int])(implicit ctx: Context) = {
    val ps = p.toSeq.sortBy(-_._2)
    entryTag(
      iconTag(""),
      div(
        ps.headOption map onePractice,
        ps match {
          case _ :: rest if rest.nonEmpty => subTag(rest map onePractice)
          case _                          => emptyFrag
        }
      )
    )
  }

  private def onePractice(tup: (lila.practice.PracticeStudy, Int))(implicit ctx: Context) =
    tup match {
      case (study, nb) =>
        val href = routes.Practice.show("-", study.slug, study.id.value)
        frag(
          trans.activity.practicedNbPositions.plural(nb, nb, a(st.href := href)(study.name)),
          br
        )
    }

  private def renderPuzzles(u: User)(p: Puzzles)(implicit ctx: Context) =
    entryTag(
      iconTag("-"),
      scoreFrag(p.score),
      div(
        trans.activity.solvedNbPuzzles.pluralSame(p.score.size),
        p.score.rp.filterNot(_.isEmpty || (u.perfs.dubiousPuzzle && !ctx.is(u))).map(ratingProgFrag)
      )
    )

  private def renderStorm(s: Storm)(implicit ctx: Context) =
    entryTag(
      iconTag("~"),
      scoreTag(winTag(trans.storm.highscoreX(strong(s.score)))),
      div(
        trans.storm.playedNbRunsOfPuzzleStorm
          .plural(s.runs, s.runs.localize, a(href := routes.Storm.home)("Puzzle Storm"))
      )
    )

  private def renderRacer(s: Racer)(implicit ctx: Context) =
    entryTag(
      iconTag(","),
      scoreTag(winTag(trans.storm.highscoreX(strong(s.score)))),
      div(
        trans.storm.playedNbRunsOfPuzzleStorm
          .plural(s.runs, s.runs.localize, a(href := routes.Racer.home)("Puzzle Racer"))
      )
    )

  private def renderStreak(s: Streak)(implicit ctx: Context) =
    entryTag(
      iconTag("}"),
      scoreTag(winTag(trans.storm.highscoreX(strong(s.score)))),
      div(
        trans.storm.playedNbRunsOfPuzzleStorm
          .plural(s.runs, s.runs.localize, a(href := routes.Puzzle.streak)("Puzzle Streak"))
      )
    )

  private def renderGames(games: Games)(implicit ctx: Context) =
    games.value.toSeq.sortBy(-_._2.size).map { case (pt, score) =>
      entryTag(
        iconTag(pt.iconChar),
        scoreFrag(score),
        div(
          trans.activity.playedNbGames.plural(score.size, score.size, pt.trans),
          score.rp.filterNot(_.isEmpty).map(ratingProgFrag)
        )
      )
    }

  private def renderPosts(posts: Map[lila.forum.Topic, List[lila.forum.Post]])(implicit ctx: Context) =
    ctx.noKid option entryTag(
      iconTag("d"),
      div(
        posts.toSeq.map { case (topic, posts) =>
          val url = routes.ForumTopic.show(topic.categId, topic.slug)
          frag(
            trans.activity.postedNbMessages
              .plural(posts.size, posts.size, a(href := url)(shorten(topic.name, 70))),
            subTag(
              posts.map { post =>
                div(cls := "line")(a(href := routes.ForumPost.redirect(post.id))(shorten(post.text, 120)))
              }
            )
          )
        }
      )
    )

  private def renderCorresMoves(nb: Int, povs: List[lila.game.LightPov])(implicit ctx: Context) =
    entryTag(
      iconTag(";"),
      div(
        trans.activity.playedNbMoves.pluralSame(nb),
        " ",
        trans.activity.inNbCorrespondenceGames.plural(povs.size, subCount(povs.size)),
        subTag(
          povs.map { pov =>
            frag(
              a(cls := "glpt", href := routes.Round.watcher(pov.gameId, pov.color.name))("Game"),
              " vs ",
              playerLink(pov.opponent, withRating = true, withDiff = false, withOnline = true, link = true),
              br
            )
          }
        )
      )
    )

  private def renderCorresEnds(score: Score, povs: List[lila.game.LightPov])(implicit ctx: Context) =
    entryTag(
      iconTag(";"),
      div(
        trans.activity.completedNbGames.plural(score.size, subCount(score.size)),
        score.rp.filterNot(_.isEmpty).map(ratingProgFrag),
        scoreFrag(score),
        subTag(
          povs.map { pov =>
            frag(
              a(cls := "glpt", href := routes.Round.watcher(pov.gameId, pov.color.name))(
                pov.game.wonBy(pov.color) match {
                  case Some(true)  => trans.victory()
                  case Some(false) => trans.defeat()
                  case _           => "Draw"
                }
              ),
              " vs ",
              playerLink(pov.opponent, withRating = true, withDiff = false, withOnline = true, link = true),
              br
            )
          }
        )
      )
    )

  private def renderFollows(all: Follows)(implicit ctx: Context) =
    entryTag(
      iconTag("h"),
      div(
        List(all.in.map(_ -> true), all.out.map(_ -> false)).flatten map { case (f, in) =>
          frag(
            if (in) trans.activity.gainedNbFollowers.pluralSame(f.actualNb)
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

  private def renderSimuls(u: User)(simuls: List[lila.simul.Simul])(implicit ctx: Context) =
    entryTag(
      iconTag("f"),
      div(
        simuls.groupBy(_.isHost(u.some)).toSeq.map { case (isHost, simuls) =>
          frag(
            if (isHost) trans.activity.hostedNbSimuls.pluralSame(simuls.size)
            else trans.activity.joinedNbSimuls.pluralSame(simuls.size),
            subTag(
              simuls.map { s =>
                div(
                  a(href := routes.Simul.show(s.id))(
                    s.name,
                    " simul by ",
                    userIdLink(s.hostId.some)
                  ),
                  scoreFrag(Score(s.wins, s.losses, s.draws, none))
                )
              }
            )
          )
        }
      )
    )

  private def renderStudies(studies: List[lila.study.Study.IdName])(implicit ctx: Context) =
    entryTag(
      iconTag("4"),
      div(
        trans.activity.createdNbStudies.pluralSame(studies.size),
        subTag(
          studies.map { s =>
            frag(a(href := routes.Study.show(s.id.value))(s.name.value), br)
          }
        )
      )
    )

  private def renderTeams(teams: Teams)(implicit ctx: Context) =
    ctx.noKid option entryTag(
      iconTag("f"),
      div(
        trans.activity.joinedNbTeams.pluralSame(teams.value.size),
        subTag(fragList(teams.value.map(id => teamLink(id))))
      )
    )

  private def renderTours(tours: lila.activity.ActivityView.Tours)(implicit ctx: Context) =
    entryTag(
      iconTag("g"),
      div(
        trans.activity.competedInNbTournaments.pluralSame(tours.nb),
        subTag(
          tours.best.map { t =>
            div(
              cls := List(
                "is-gold" -> (t.rank == 1),
                "text"    -> (t.rank <= 3)
              ),
              dataIcon := (t.rank <= 3).option("g")
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
          }
        )
      )
    )

  private def renderSwisses(swisses: List[(Swiss.IdName, Int)])(implicit ctx: Context) =
    entryTag(
      iconTag("g"),
      div(
        trans.activity.competedInNbSwissTournaments.pluralSame(swisses.size),
        subTag(
          swisses.map { case (swiss, rank) =>
            div(
              cls := List(
                "is-gold" -> (rank == 1),
                "text"    -> (rank <= 3)
              ),
              dataIcon := (rank <= 3).option("g")
            )(
              trans.activity.rankedInSwissTournament(
                strong(rank),
                a(href := routes.Swiss.show(swiss.id.value))(swiss.name)
              ),
              br
            )
          }
        )
      )
    )

  private def renderStream(u: User)(implicit ctx: Context) =
    ctx.noKid option entryTag(
      iconTag(""),
      a(href := routes.Streamer.redirect(u.username))(trans.activity.hostedALiveStream())
    )

  private def renderSignup(implicit ctx: Context) =
    entryTag(
      iconTag("s"),
      div(trans.activity.signedUp())
    )

  private val entryTag = div(cls := "entry")
  private val subTag   = div(cls := "sub")
  private val scoreTag = tag("score")
  private val winTag   = tag("win")

  private def scoreFrag(s: Score)(implicit ctx: Context) =
    raw {
      s"""<score>${scoreStr("win", s.win, trans.nbWins)}${scoreStr("draw", s.draw, trans.nbDraws)}${scoreStr(
        "loss",
        s.loss,
        trans.nbLosses
      )}</score>"""
    }

  private def ratingProgFrag(r: RatingProg) =
    ratingTag(r.after.value, ratingProgress(r.diff))

  private def scoreStr(tag: String, p: Int, name: lila.i18n.I18nKey)(implicit ctx: Context) =
    if (p == 0) ""
    else s"""<$tag>${wrapNumber(name.pluralSameTxt(p))}</$tag>"""

  private val wrapNumberRegex         = """(\d++)""".r
  private def wrapNumber(str: String) = wrapNumberRegex.replaceAllIn(str, "<strong>$1</strong>")
}
