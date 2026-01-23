package lila.puzzle

import play.api.libs.json.{ Json, JsObject }

import lila.ui.Context
import lila.core.i18n.Translate
import lila.common.Json.given

final class PuzzleComplete(
    api: PuzzleApi,
    finisher: PuzzleFinisher,
    session: PuzzleSessionApi,
    selector: PuzzleSelector,
    replayApi: PuzzleReplayApi,
    jsonView: JsonView
)(using Executor):

  def onComplete[A](
      data: PuzzleForm.RoundData
  )(id: PuzzleId, angle: PuzzleAngle, mobileBc: Boolean)(using
      ctx: Context
  )(using Perf, Translate): Fu[JsObject] =
    given Option[Me] = ctx.me
    data.streakPuzzleId.match
      case Some(streakNextId) =>
        api.puzzle
          .find(streakNextId)
          .flatMap:
            case None => fuccess(Json.obj("streakComplete" -> true))
            case Some(puzzle) =>
              for
                score <- data.streakScore
                if data.win.no
                if score > 0
                _ = lila.mon.streak.run.score(ctx.isAuth.toString).record(score)
                userId <- ctx.userId
              do setStreakResult(userId, score)
              jsonView.analysis(puzzle, angle).map { nextJson =>
                Json.obj("next" -> nextJson)
              }
      case None =>
        lila.mon.puzzle.round.attempt(ctx.isAuth, angle.key, data.rated.yes).increment()
        ctx.me match
          case Some(me) =>
            given Me = me
            finisher(id, angle, data.win, data.rated).flatMapz { (round, perf) =>
              val newMe = me.value.withPerf(perf)
              for
                _ <- session.onComplete(me.userId, angle)
                json <-
                  if mobileBc then
                    fuccess:
                      jsonView.bc.userJson(perf.intRating) ++ Json.obj(
                        "round" -> Json.obj(
                          "ratingDiff" -> 0,
                          "win" -> data.win
                        ),
                        "voted" -> round.vote
                      )
                  else
                    (data.replayDays, angle.asTheme) match
                      case (Some(replayDays), Some(theme)) =>
                        for
                          _ <- replayApi.onComplete(round, replayDays, angle)
                          next <- replayApi(replayDays.some, theme)
                          json <- next match
                            case None => fuccess(Json.obj("replayComplete" -> true))
                            case Some(puzzle, replay) =>
                              jsonView.analysis(puzzle, angle, replay.some).map { nextJson =>
                                Json.obj(
                                  "round" -> jsonView.roundJson.web(round, perf),
                                  "next" -> nextJson
                                )
                              }
                        yield json
                      case _ =>
                        for
                          next <- selector.nextPuzzleFor(
                            angle,
                            none,
                            PuzzleDifficulty.fromReqSession(ctx.req)
                          )
                          nextJson <- next.traverse:
                            given Perf = perf
                            jsonView.analysis(_, angle, none, Me.from(newMe.user.some))
                        yield Json.obj(
                          "round" -> jsonView.roundJson.web(round, perf),
                          "next" -> nextJson
                        )
              yield json
            }
          case None =>
            finisher.incPuzzlePlays(id)
            if mobileBc then fuccess(Json.obj("user" -> false))
            else
              selector
                .nextPuzzleFor(angle, data.color.map(some), PuzzleDifficulty.fromReqSession(ctx.req))
                .flatMap:
                  _.so(jsonView.analysis(_, angle))
                .map: json =>
                  Json.obj("next" -> json)

  def setStreakResult(userId: UserId, score: Int) =
    lila.common.Bus.pub(lila.core.misc.puzzle.StreakRun(userId, score))
