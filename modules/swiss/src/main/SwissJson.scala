package lila.swiss

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ GreatPlayer, LightUser, Uptime }
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.quote.Quote.quoteWriter
import lila.rating.PerfType
import lila.socket.Socket.SocketVersion
import lila.user.User

final class SwissJson(
    colls: SwissColls,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def apply(
      swiss: Swiss,
      leaderboard: List[LeaderboardPlayer],
      me: Option[User],
      socketVersion: Option[SocketVersion]
  )(implicit lang: Lang): Fu[JsObject] =
    me.?? { fetchMyInfo(swiss, _) } map { myInfo =>
      Json
        .obj(
          "id"        -> swiss.id.value,
          "createdBy" -> swiss.createdBy,
          "startsAt"  -> formatDate(swiss.startsAt),
          "name"      -> swiss.name,
          "perf"      -> swiss.perfType,
          "clock"     -> swiss.clock,
          "variant"   -> swiss.variant.key,
          "round"     -> swiss.round,
          "nbRounds"  -> swiss.nbRounds,
          "nbPlayers" -> swiss.nbPlayers,
          "status" -> {
            if (swiss.isStarted) "started"
            else if (swiss.isFinished) "finished"
            else "created"
          },
          "leaderboard" -> leaderboard.map { l =>
            Json
              .obj(
                "user"   -> lightUserApi.sync(l.player.userId),
                "rating" -> l.player.rating,
                "points" -> l.player.points,
                "score"  -> l.player.score,
                "pairings" -> swiss.allRounds.map(l.pairings.get).map {
                  _.fold[JsValue](JsNull) { p =>
                    Json.obj(
                      "o" -> p.opponentOf(l.player.number),
                      "g" -> p.gameId,
                      "w" -> p.winner.map(l.player.number.==)
                    )
                  }
                }
              )
              .add("provisional" -> l.player.provisional)
          }
        )
        .add("isFinished" -> swiss.isFinished)
        .add("socketVersion" -> socketVersion.map(_.value))
        .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
        .add("description" -> swiss.description)
        .add("secondsToStart" -> swiss.isCreated.option(swiss.secondsToStart))
        .add("me" -> myInfo.map(myInfoJson))
        .add("greatPlayer" -> GreatPlayer.wikiUrl(swiss.name).map { url =>
          Json.obj("name" -> swiss.name, "url" -> url)
        })
    }

  def fetchMyInfo(swiss: Swiss, me: User): Fu[Option[MyInfo]] =
    colls.swiss.one[SwissPlayer]($doc("s" -> swiss.id, "u" -> me.id)) flatMap {
      _ ?? { player =>
        colls.pairing
          .find(
            $doc("s" -> swiss.id, "u" -> me.id),
            $doc("_id" -> true).some
          )
          .sort($sort desc "d")
          .one[Bdoc]
          .dmap { _.flatMap(_.getAsOpt[Game.ID]("_id")) }
          .flatMap { gameId =>
            getOrGuessRank(swiss, player) dmap { rank =>
              MyInfo(rank + 1, false, gameId, me).some
            }
          }
      }
    }

  private[swiss] def playerJson(
      rankedPlayer: RankedPlayer,
      pairings: Map[SwissRound.Number, SwissPairing]
  )(implicit ec: ExecutionContext): Fu[JsObject] = {
    val p = rankedPlayer.player
    lightUserApi async p.userId map { light =>
      Json
        .obj(
          "name"   -> light.fold(p.userId)(_.name),
          "rank"   -> rankedPlayer.rank,
          "rating" -> p.rating,
          "score"  -> p.score,
          "sheet"  -> sheet.map(sheetJson)
        )
        .add("title" -> light.flatMap(_.title))
        .add("provisional" -> p.provisional)
        .add("withdraw" -> p.withdraw)
        .add("team" -> p.team)
    }
  }

  // if the user is not yet in the cached ranking,
  // guess its rank based on other players scores in the DB
  private def getOrGuessRank(swiss: Swiss, player: SwissPlayer): Fu[Int] = ???
  // cached ranking swiss flatMap {
  //   _ get player.userId match {
  //     case Some(rank) => fuccess(rank)
  //     case None       => playerRepo.computeRankOf(player)
  //   }
  // }

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date

  private def myInfoJson(i: MyInfo) =
    Json
      .obj(
        "rank"     -> i.rank,
        "withdraw" -> i.withdraw,
        "gameId"   -> i.gameId,
        "id"       -> i.user.id
      )

  implicit private val roundNumberWriter: Writes[SwissRound.Number] = Writes[SwissRound.Number] { n =>
    JsNumber(n.value)
  }
  implicit private val playerNumberWriter: Writes[SwissPlayer.Number] = Writes[SwissPlayer.Number] { n =>
    JsNumber(n.value)
  }
  implicit private val pointsWriter: Writes[Swiss.Points] = Writes[Swiss.Points] { p =>
    JsNumber(p.value)
  }
  implicit private val scoreWriter: Writes[Swiss.Score] = Writes[Swiss.Score] { s =>
    JsNumber(s.value)
  }

  implicit private val pairingWrites: OWrites[SwissPairing] = OWrites { p =>
    Json.obj(
      "gameId" -> p.gameId,
      "white"  -> p.white,
      "black"  -> p.black,
      "winner" -> p.winner
    )
  }

  implicit private val clockWrites: OWrites[chess.Clock.Config] = OWrites { clock =>
    Json.obj(
      "limit"     -> clock.limitSeconds,
      "increment" -> clock.incrementSeconds
    )
  }

  implicit private def perfTypeWrites(implicit lang: Lang): OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "icon" -> pt.iconChar.toString,
      "name" -> pt.trans
    )
  }
}
