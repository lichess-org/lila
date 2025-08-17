package lila.activity

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.activity.activities.*
import lila.common.Json.{ *, given }
import lila.core.game.LightPov
import lila.core.rating.{ RatingProg, Score }
import lila.core.simul.Simul
import lila.core.tournament.leaderboard.Ratio
import lila.rating.PerfType

final class JsonView(
    getTourName: lila.core.tournament.GetTourName,
    getLightTeam: lila.core.team.LightTeam.GetterSync
):

  private object Writers:
    given OWrites[TimeInterval] = OWrites: i =>
      Json.obj("start" -> i.start, "end" -> i.end)
    given Writes[PerfType] = writeAs(_.key)
    given Writes[RatingProg] = Json.writes
    given Writes[Score] = Json.writes
    given OWrites[Games] = OWrites: games =>
      JsObject:
        games.value.toList
          .sortBy((_, s) => -s.size)
          .map: (pk, score) =>
            pk.value -> Json.toJson(score)

    given Writes[chess.variant.Variant] = writeAs(_.key)

    // writes as percentage
    given Writes[Ratio] = Writes: r =>
      JsNumber((r.value * 100).toInt.atLeast(1))

    given (using Lang): OWrites[lila.core.tournament.leaderboard.Entry] = OWrites: e =>
      val name = getTourName.sync(e.tourId).orZero
      Json.obj(
        "tournament" -> Json.obj(
          "id" -> e.tourId,
          "name" -> name
        ),
        "nbGames" -> e.nbGames,
        "score" -> e.score,
        "rank" -> e.rank,
        "rankPercent" -> e.rankRatio
      )
    given (using Lang): Writes[ActivityView.Tours] = Json.writes
    given Writes[Puzzles] = writeWrap("score")(_.value)
    given Writes[Storm] = Json.writes
    given Writes[Racer] = Json.writes
    given Writes[Streak] = Json.writes
    def simulWrites(user: User) = OWrites[Simul]: s =>
      Json.obj(
        "id" -> s.id,
        "name" -> s.name,
        "isHost" -> (s.hostId == user.id),
        "variants" -> s.variants,
        "score" -> s.hostScore
      )
    given lightPlayerWrites: OWrites[lila.core.game.LightPlayer] = OWrites: p =>
      Json
        .obj()
        .add("aiLevel" -> p.aiLevel)
        .add("user" -> p.userId)
        .add("rating" -> p.rating)

    given OWrites[lila.core.game.Player] = lightPlayerWrites.contramap(_.light)

    given OWrites[LightPov] = OWrites: p =>
      Json.obj(
        "id" -> p.game.id,
        "color" -> p.color,
        "url" -> s"/${p.game.id}/${p.color.name}",
        "opponent" -> p.opponent
      )
    given Writes[FollowList] = Json.writes
    given Writes[Follows] = Json.writes
    given Writes[Teams] = Writes: s =>
      JsArray(s.value.flatMap(getLightTeam(_)).map { team =>
        Json.obj("url" -> s"/team/${team.id}", "name" -> team.name).add("flair" -> team.flair)
      })
    given Writes[Patron] = Json.writes
  import Writers.{ *, given }

  private given OWrites[lila.core.study.IdName] = Json.writes
  def apply(a: ActivityView, user: User)(using Lang): Fu[JsObject] =
    fuccess:
      Json
        .obj("interval" -> a.interval)
        .add("games", a.games)
        .add("puzzles", a.puzzles)
        .add("storm", a.storm)
        .add("racer", a.racer)
        .add("streak", a.streak)
        .add("tournaments", a.tours)
        .add(
          "practice",
          a.practice.map(_.toList.sortBy(-_._2).map { (study, nb) =>
            Json.obj(
              "url" -> s"/practice/-/${study.slug}/${study.id}",
              "name" -> study.name,
              "nbPositions" -> nb
            )
          })
        )
        .add("simuls", a.simuls.map(_.map(simulWrites(user).writes)))
        .add(
          "correspondenceMoves",
          a.corresMoves.map: (nb, povs) =>
            Json.obj("nb" -> nb, "games" -> povs)
        )
        .add(
          "correspondenceEnds",
          a.corresEnds.map:
            _.map { case (pk, (score, povs)) =>
              pk.value -> Json.obj("score" -> score, "games" -> povs)
            }
        )
        .add("follows" -> a.follows)
        .add("studies" -> a.studies)
        .add("teams" -> a.teams)
        .add("posts" -> a.forumPosts.map(_.map { (topic, posts) =>
          Json.obj(
            "topicUrl" -> s"/forum/${topic.categId}/${topic.slug}",
            "topicName" -> topic.name,
            "posts" -> posts.map { p =>
              Json.obj(
                "url" -> s"/forum/redirect/post/${p.id}",
                "text" -> p.text.take(500)
              )
            }
          )
        }))
        .add("patron" -> a.patron)
        .add("stream" -> a.stream)
