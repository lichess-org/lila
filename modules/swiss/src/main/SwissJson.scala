package lila.swiss

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.LightUser
import lila.common.Json.given
import lila.db.dsl.{ *, given }
import lila.quote.Quote.given
import lila.socket.{ SocketVersion, given }
import lila.user.{ User, UserRepo }
import lila.gathering.Condition.WithVerdicts
import lila.gathering.GreatPlayer

final class SwissJson(
    mongo: SwissMongo,
    standingApi: SwissStandingApi,
    rankingApi: SwissRankingApi,
    boardApi: SwissBoardApi,
    statsApi: SwissStatsApi,
    userRepo: UserRepo,
    lightUserApi: lila.user.LightUserApi
)(using Executor):

  import SwissJson.{ *, given }
  import BsonHandlers.given

  def api(swiss: Swiss) = statsApi(swiss) map { stats =>
    swissJsonBase(swiss) ++ Json.obj(
      "stats" -> stats,
      "rated" -> swiss.settings.rated
    )
  }

  def apply(
      swiss: Swiss,
      me: Option[User],
      isInTeam: Boolean,
      verdicts: WithVerdicts,
      reqPage: Option[Int] = None, // None = focus on me
      socketVersion: Option[SocketVersion] = None,
      playerInfo: Option[SwissPlayer.ViewExt] = None
  )(using lang: Lang): Fu[JsObject] = {
    for {
      myInfo <- me.so { fetchMyInfo(swiss, _) }
      page = reqPage orElse myInfo.map(_.page) getOrElse 1
      standing <- standingApi(swiss, page)
      podium   <- podiumJson(swiss)
      boards   <- boardApi(swiss.id)
      stats    <- statsApi(swiss)
    } yield swissJsonBase(swiss) ++ Json
      .obj(
        "canJoin" -> {
          {
            (swiss.isNotFinished && myInfo.exists(_.player.absent)) ||
            (myInfo.isEmpty && swiss.isEnterable)
          } && verdicts.accepted && isInTeam
        },
        "standing" -> standing,
        "boards"   -> boards.map(boardJson)
      )
      .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
      .add("me" -> myInfo.map(myInfoJson))
      .add("joinTeam" -> (!isInTeam).option(swiss.teamId))
      .add("socketVersion" -> socketVersion)
      .add("playerInfo" -> playerInfo.map { playerJsonExt(swiss, _) })
      .add("podium" -> podium)
      .add("stats" -> stats)
      .add("greatPlayer" -> GreatPlayer.wikiUrl(swiss.name).map { url =>
        Json.obj("name" -> swiss.name, "url" -> url)
      })
  }.monSuccess(_.swiss.json)

  def fetchMyInfo(swiss: Swiss, me: User): Fu[Option[MyInfo]] =
    mongo.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, me.id).value) flatMapz { player =>
      updatePlayerRating(swiss, player, me) >>
        SwissPairing.fields { f =>
          (swiss.nbOngoing > 0)
            .so {
              mongo.pairing
                .find(
                  $doc(f.swissId -> swiss.id, f.players -> player.userId, f.status -> SwissPairing.ongoing),
                  $doc(f.id -> true).some
                )
                .one[Bdoc]
                .dmap { _.flatMap(_.getAsOpt[GameId](f.id)) }
            }
            .flatMap { gameId =>
              rankingApi(swiss).dmap(_ get player.userId) map2 {
                MyInfo(_, gameId, me, player)
              }
            }
        }
    }

  private def updatePlayerRating(swiss: Swiss, player: SwissPlayer, user: User): Funit =
    swiss.settings.rated
      .option(user perfs swiss.perfType)
      .filter(_.intRating != player.rating)
      .so { perf =>
        SwissPlayer.fields { f =>
          mongo.player.update
            .one(
              $id(SwissPlayer.makeId(swiss.id, user.id)),
              $set(f.rating -> perf.intRating)
            )
            .void
        }
      }

  private def podiumJson(swiss: Swiss): Fu[Option[JsArray]] =
    swiss.isFinished so {
      SwissPlayer.fields { f =>
        mongo.player
          .find($doc(f.swissId -> swiss.id))
          .sort($sort desc f.score)
          .cursor[SwissPlayer]()
          .list(3) flatMap { top3 =>
          // check that the winner is still correctly denormalized
          top3.headOption
            .map(_.userId)
            .filter(w => swiss.winnerId.fold(true)(w !=))
            .foreach {
              mongo.swiss.updateField($id(swiss.id), "winnerId", _).void
            }
            .unit
          userRepo.filterLame(top3.map(_.userId)) map { lame =>
            JsArray(
              top3.map { player =>
                playerJsonBase(
                  player,
                  lightUserApi.syncFallback(player.userId),
                  performance = true
                ).add("lame", lame(player.userId))
              }
            ).some
          }
        }
      }
    }

  def playerResult(p: SwissPlayer.WithUserAndRank): JsObject = p match
    case SwissPlayer.WithUserAndRank(player, user, rank) =>
      Json
        .obj(
          "rank"     -> rank,
          "points"   -> player.points.value,
          "tieBreak" -> player.tieBreak.value,
          "rating"   -> player.rating,
          "username" -> user.name
        )
        .add("title" -> user.title)
        .add("performance" -> player.performance)
        .add("absent" -> player.absent)

object SwissJson:

  private def swissJsonBase(swiss: Swiss) =
    Json
      .obj(
        "id"        -> swiss.id,
        "createdBy" -> swiss.createdBy,
        "startsAt"  -> isoDateTimeFormatter.print(swiss.startsAt),
        "name"      -> swiss.name,
        "clock"     -> swiss.clock,
        "variant"   -> swiss.variant.key,
        "round"     -> swiss.round,
        "nbRounds"  -> swiss.settings.nbRounds,
        "nbPlayers" -> swiss.nbPlayers,
        "nbOngoing" -> swiss.nbOngoing,
        "status" -> {
          if (swiss.isStarted) "started"
          else if (swiss.isFinished) "finished"
          else "created"
        }
      )
      .add("nextRound" -> swiss.nextRoundAt.map { next =>
        Json.obj(
          "at" -> isoDateTimeFormatter.print(next),
          "in" -> (next.toSeconds - nowSeconds).toInt.atLeast(0)
        )
      })
      .add("isRecentlyFinished" -> swiss.isRecentlyFinished)
      .add("password" -> swiss.settings.password.isDefined)

  private[swiss] def playerJson(swiss: Swiss, view: SwissPlayer.View): JsObject =
    playerJsonBase(view, performance = false) ++ Json
      .obj(
        "sheetMin" -> swiss.allRounds
          .map(view.pairings.get)
          .zip(view.sheet.outcomes)
          .map {
            pairingJsonOrOutcome(view.player)
          }
          .mkString("|")
      )

  def playerJsonExt(swiss: Swiss, view: SwissPlayer.ViewExt): JsObject =
    playerJsonBase(view, performance = true) ++ Json.obj(
      "sheet" -> swiss.allRounds
        .zip(view.sheet.outcomes)
        .reverse
        .map { case (round, outcome) =>
          view.pairings.get(round).fold[JsValue](JsString(outcomeJson(outcome))) { p =>
            pairingJson(view.player, p.pairing) ++
              Json.obj(
                "user"   -> p.player.user,
                "rating" -> p.player.player.rating
              )
          }
        }
    )

  private def playerJsonBase(
      view: SwissPlayer.Viewish,
      performance: Boolean
  ): JsObject =
    playerJsonBase(view.player, view.user, performance) ++
      Json.obj("rank" -> view.rank)

  private def playerJsonBase(
      p: SwissPlayer,
      user: LightUser,
      performance: Boolean
  ): JsObject =
    Json
      .obj(
        "user"     -> user,
        "rating"   -> p.rating,
        "points"   -> p.points,
        "tieBreak" -> p.tieBreak
      )
      .add("performance" -> (performance so p.performance))
      .add("provisional" -> p.provisional)
      .add("absent" -> p.absent)

  private def outcomeJson(outcome: SwissSheet.Outcome): String =
    outcome match
      case SwissSheet.Outcome.Absent => "absent"
      case SwissSheet.Outcome.Late   => "late"
      case SwissSheet.Outcome.Bye    => "bye"
      case _                         => ""

  private def pairingJsonMin(player: SwissPlayer, pairing: SwissPairing): String =
    val status =
      if (pairing.isOngoing) "o"
      else pairing.resultFor(player.userId).fold("d") { r => if (r) "w" else "l" }
    s"${pairing.gameId}$status"

  private def pairingJson(player: SwissPlayer, pairing: SwissPairing) =
    Json
      .obj(
        "g" -> pairing.gameId
      )
      .add("o" -> pairing.isOngoing)
      .add("w" -> pairing.resultFor(player.userId))
      .add("c" -> (pairing.white == player.userId))

  private def pairingJsonOrOutcome(
      player: SwissPlayer
  ): ((Option[SwissPairing], SwissSheet.Outcome)) => String =
    case (Some(pairing), _) => pairingJsonMin(player, pairing)
    case (_, outcome)       => outcomeJson(outcome)

  private def myInfoJson(i: MyInfo) =
    Json
      .obj(
        "rank"   -> i.rank,
        "gameId" -> i.gameId,
        "id"     -> i.user.id,
        "name"   -> i.user.username,
        "absent" -> i.player.absent
      )

  private[swiss] def boardJson(b: SwissBoard.WithGame) =
    Json
      .obj(
        "id"          -> b.game.id,
        "fen"         -> chess.format.Fen.writeBoardAndColor(b.game.situation),
        "lastMove"    -> (b.game.lastMoveKeys | ""),
        "orientation" -> b.game.naturalOrientation.name,
        "white"       -> boardPlayerJson(b.board.white),
        "black"       -> boardPlayerJson(b.board.black)
      )
      .add(
        "clock" -> b.game.clock.ifTrue(b.game.isBeingPlayed).map { c =>
          Json.obj(
            "white" -> c.remainingTime(chess.White).roundSeconds,
            "black" -> c.remainingTime(chess.Black).roundSeconds
          )
        }
      )
      .add("winner" -> b.game.winnerColor.map(_.name))

  private def boardPlayerJson(player: SwissBoard.Player) =
    Json.obj(
      "rank"   -> player.rank,
      "rating" -> player.rating,
      "user"   -> player.user
    )

  private given Writes[SwissRoundNumber]  = Writes(n => JsNumber(n.value))
  private given Writes[SwissPoints]       = Writes(p => JsNumber(BigDecimal(p.value)))
  private given Writes[Swiss.TieBreak]    = Writes(t => JsNumber(t.value))
  private given Writes[Swiss.Performance] = Writes(t => JsNumber(t.value.toInt))

  private given OWrites[chess.Clock.Config] = OWrites { clock =>
    Json.obj(
      "limit"     -> clock.limitSeconds,
      "increment" -> clock.incrementSeconds
    )
  }

  private given Writes[SwissStats] = Json.writes
