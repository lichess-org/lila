package lila.swiss

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.chat.Chat
import lila.common.{ Bus, GreatPlayer, WorkQueues }
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.round.actorApi.round.QuietFlag
import lila.user.User

final class SwissApi(
    colls: SwissColls,
    socket: SwissSocket,
    director: SwissDirector,
    scoring: SwissScoring,
    chatApi: lila.chat.ChatApi
)(
    implicit ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
) {

  private val workQueue =
    new WorkQueues(buffer = 128, expiration = 1 minute, timeout = 10 seconds, name = "swiss")

  import BsonHandlers._

  def byId(id: Swiss.Id)            = colls.swiss.byId[Swiss](id.value)
  def notFinishedById(id: Swiss.Id) = byId(id).dmap(_.filter(_.isNotFinished))
  def createdById(id: Swiss.Id)     = byId(id).dmap(_.filter(_.isCreated))
  def startedById(id: Swiss.Id)     = byId(id).dmap(_.filter(_.isStarted))

  def create(data: SwissForm.SwissData, me: User, teamId: TeamID): Fu[Swiss] = {
    val swiss = Swiss(
      _id = Swiss.makeId,
      name = data.name | GreatPlayer.randomName,
      clock = data.clock,
      variant = data.realVariant,
      rated = data.rated | true,
      round = SwissRound.Number(0),
      nbRounds = data.nbRounds,
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = DateTime.now,
      createdBy = me.id,
      teamId = teamId,
      nextRoundAt = data.realStartsAt.some,
      startsAt = data.realStartsAt,
      finishedAt = none,
      winnerId = none,
      description = data.description,
      hasChat = data.hasChat | true
    )
    colls.swiss.insert.one(swiss) inject swiss
  }

  def update(old: Swiss, data: SwissForm.SwissData): Funit = {
    val swiss = old.copy(
      name = data.name | old.name,
      clock = data.clock,
      variant = data.realVariant,
      rated = data.rated | old.rated,
      nbRounds = data.nbRounds,
      startsAt = data.startsAt.ifTrue(old.isCreated) | old.startsAt,
      nextRoundAt = if (old.isCreated) Some(data.startsAt | old.startsAt) else old.nextRoundAt,
      description = data.description,
      hasChat = data.hasChat | old.hasChat
    )
    colls.swiss.update.one($id(swiss.id), swiss).void
  }

  def join(id: Swiss.Id, me: User, isInTeam: TeamID => Boolean): Fu[Boolean] =
    Sequencing(id)(notFinishedById) { swiss =>
      (swiss.isEnterable && isInTeam(swiss.teamId)) ?? {
        val number = SwissPlayer.Number(swiss.nbPlayers + 1)
        colls.player.insert.one(SwissPlayer.make(swiss.id, number, me, swiss.perfLens)) zip
          colls.swiss.updateField($id(swiss.id), "nbPlayers", number) >>
            scoring.recompute(swiss) >>-
            socket.reload(swiss.id) inject true
      }
    }

  def pairingsOf(swiss: Swiss) = SwissPairing.fields { f =>
    colls.pairing.ext
      .find($doc(f.swissId -> swiss.id))
      .sort($sort asc f.round)
      .list[SwissPairing]()
  }

  def featuredInTeam(teamId: TeamID): Fu[List[Swiss]] =
    colls.swiss.ext.find($doc("teamId" -> teamId)).sort($sort desc "startsAt").list[Swiss](5)

  private[swiss] def finishGame(game: Game): Funit = game.swissId ?? { swissId =>
    Sequencing(Swiss.Id(swissId))(startedById) { swiss =>
      colls.pairing.byId[SwissPairing](game.id).dmap(_.filter(_.isOngoing)) flatMap {
        _ ?? { pairing =>
          val winner = game.winnerColor
            .map(_.fold(pairing.white, pairing.black))
            .flatMap(playerNumberHandler.writeOpt)
          colls.pairing.updateField($id(game.id), SwissPairing.Fields.status, winner | BSONNull).void >>
            colls.swiss.update.one($id(swiss.id), $inc("nbOngoing" -> -1)) >>
            scoring.recompute(swiss) >> {
            if (swiss.round.value == swiss.nbRounds) doFinish(swiss)
            else if (swiss.nbOngoing == 1) {
              val minutes = 1
              colls.swiss
                .updateField($id(swiss.id), "nextRoundAt", DateTime.now.plusMinutes(minutes))
                .void >>-
                systemChat(
                  swiss.id,
                  s"Round ${swiss.round.value + 1} will start soon."
                )
            } else funit
          } >>- socket.reload(swiss.id)
        }
      }
    }
  }

  // private def isCurrentRoundFinished(swiss: Swiss) =
  //   SwissPairing
  //     .fields { f =>
  //       !colls.pairing.exists(
  //         $doc(f.swissId -> swiss.id, f.round -> swiss.round, f.status -> SwissPairing.ongoing)
  //       )
  //     }

  private[swiss] def destroy(swiss: Swiss): Funit =
    colls.swiss.delete.one($id(swiss.id)) >>
      colls.pairing.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id)) >>
      colls.player.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id)).void >>-
      socket.reload(swiss.id)

  private[swiss] def finish(oldSwiss: Swiss): Funit =
    Sequencing(oldSwiss.id)(startedById) { swiss =>
      colls.pairing.countSel($doc(SwissPairing.Fields.swissId -> swiss.id)) flatMap {
        case 0 => destroy(swiss)
        case _ => doFinish(swiss: Swiss)
      }
    }
  private def doFinish(swiss: Swiss): Funit =
    for {
      _ <- colls.swiss.update
        .one(
          $id(swiss.id),
          $unset("nextRoundAt") ++ $set(
            "nbRounds"   -> swiss.round,
            "finishedAt" -> DateTime.now
          )
        )
        .void
      winner <- SwissPlayer.fields { f =>
        colls.player.ext.find($doc(f.swissId -> swiss.id)).sort($sort desc f.score).one[SwissPlayer]
      }
      _ <- winner.?? { p =>
        colls.swiss.updateField($id(swiss.id), "winnerId", p.userId).void
      }
    } yield socket.reload(swiss.id)

  def kill(swiss: Swiss): Funit = {
    if (swiss.isStarted) finish(swiss)
    else if (swiss.isCreated) destroy(swiss)
    else funit
  }

  private[swiss] def startPendingRounds: Funit =
    colls.swiss.ext
      .find($doc("nextRoundAt" $lt DateTime.now), $id(true))
      .list[Bdoc](10)
      .map(_.flatMap(_.getAsOpt[Swiss.Id]("_id")))
      .flatMap { ids =>
        lila.common.Future.applySequentially(ids) { id =>
          Sequencing(id)(notFinishedById) { swiss =>
            val fu =
              if (swiss.nbPlayers >= 4)
                director.startRound(swiss).flatMap {
                  _.fold(
                    doFinish(swiss) >>-
                      systemChat(
                        swiss.id,
                        s"Not enough players for round ${swiss.round.value + 1}; terminating tournament."
                      )
                  ) { s =>
                    scoring.recompute(s) >>-
                      systemChat(
                        swiss.id,
                        s"Round ${swiss.round.value + 1} started."
                      )
                  }
                } else {
                if (swiss.startsAt isBefore DateTime.now.minusMinutes(60)) destroy(swiss)
                else {
                  systemChat(swiss.id, "Not enough players for first round; delaying start.", true)
                  colls.swiss.update
                    .one($id(swiss.id), $set("nextRoundAt" -> DateTime.now.plusSeconds(21)))
                    .void
                }
              }
            fu >>- socket.reload(swiss.id)
          }
        }
      }
      .monSuccess(_.swiss.tick)

  private[swiss] def checkOngoingGames: Funit =
    SwissPairing.fields { f =>
      colls.pairing.primitive[Game.ID]($doc(f.status -> SwissPairing.ongoing), f.id)
    } map { gameIds =>
      Bus.publish(lila.hub.actorApi.map.TellMany(gameIds, QuietFlag), "roundSocket")
    }

  private def systemChat(id: Swiss.Id, text: String, volatile: Boolean = false): Unit =
    chatApi.userChat.service(
      Chat.Id(id.value),
      text,
      _.Swiss,
      volatile
    )

  private def Sequencing[A: Zero](
      id: Swiss.Id
  )(fetch: Swiss.Id => Fu[Option[Swiss]])(run: Swiss => Fu[A]): Fu[A] =
    workQueue(id.value) {
      fetch(id) flatMap {
        _ ?? run
      }
    }
}
