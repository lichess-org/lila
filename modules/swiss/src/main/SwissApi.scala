package lila.swiss

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.chat.Chat
import lila.common.{ Bus, GreatPlayer, LightUser, WorkQueues }
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.round.actorApi.round.QuietFlag
import lila.user.{ User, UserRepo }

final class SwissApi(
    colls: SwissColls,
    cache: SwissCache,
    userRepo: UserRepo,
    socket: SwissSocket,
    director: SwissDirector,
    scoring: SwissScoring,
    chatApi: lila.chat.ChatApi,
    lightUserApi: lila.user.LightUserApi,
    roundSocket: lila.round.RoundSocket
)(implicit
    ec: scala.concurrent.ExecutionContext,
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
      round = SwissRound.Number(0),
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = DateTime.now,
      createdBy = me.id,
      teamId = teamId,
      nextRoundAt = data.realStartsAt.some,
      startsAt = data.realStartsAt,
      finishedAt = none,
      winnerId = none,
      settings = Swiss.Settings(
        nbRounds = data.nbRounds,
        rated = data.rated | true,
        description = data.description,
        hasChat = data.hasChat | true,
        roundInterval = data.realRoundInterval
      )
    )
    colls.swiss.insert.one(swiss) inject swiss
  }

  def update(old: Swiss, data: SwissForm.SwissData): Funit = {
    val swiss = old.copy(
      name = data.name | old.name,
      clock = data.clock,
      variant = data.realVariant,
      startsAt = data.startsAt.ifTrue(old.isCreated) | old.startsAt,
      nextRoundAt = if (old.isCreated) Some(data.startsAt | old.startsAt) else old.nextRoundAt,
      settings = old.settings.copy(
        nbRounds = data.nbRounds,
        rated = data.rated | old.settings.rated,
        description = data.description,
        hasChat = data.hasChat | old.settings.hasChat,
        roundInterval = data.roundInterval.fold(old.settings.roundInterval)(_.seconds)
      )
    )
    colls.swiss.update.one($id(swiss.id), swiss).void
  }

  def join(id: Swiss.Id, me: User, isInTeam: TeamID => Boolean): Fu[Boolean] =
    Sequencing(id)(notFinishedById) { swiss =>
      colls.player // try a rejoin first
        .updateField($id(SwissPlayer.makeId(swiss.id, me.id)), SwissPlayer.Fields.absent, false)
        .flatMap { rejoin =>
          fuccess(rejoin.nModified == 1) >>| { // if it failed, try a join
            (swiss.isEnterable && isInTeam(swiss.teamId)) ?? {
              val number = SwissPlayer.Number(swiss.nbPlayers + 1)
              colls.player.insert.one(SwissPlayer.make(swiss.id, number, me, swiss.perfLens)) zip
                colls.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> 1)) inject true
            }
          } flatMap { res =>
            scoring.recompute(swiss) >>- socket.reload(swiss.id) inject res
          }
        } addEffect { _ => }
    }

  def withdraw(id: Swiss.Id, me: User): Funit =
    Sequencing(id)(notFinishedById) { swiss =>
      SwissPlayer.fields { f =>
        if (swiss.isStarted)
          colls.player.updateField($id(SwissPlayer.makeId(swiss.id, me.id)), f.absent, true)
        else
          colls.player.ext.findAndRemove[SwissPlayer]($id(SwissPlayer.makeId(swiss.id, me.id))) flatMap {
            _ ?? { player =>
              colls.player.update
                .one(
                  $doc(f.swissId -> id, f.number $gt player.number),
                  $inc(f.number  -> -1),
                  multi = true
                ) zip
                colls.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> -1)) void
            }
          }
      }.void >>- socket.reload(swiss.id)
    }

  def pairingsOf(swiss: Swiss) =
    SwissPairing.fields { f =>
      colls.pairing.ext
        .find($doc(f.swissId -> swiss.id))
        .sort($sort asc f.round)
        .list[SwissPairing]()
    }

  def featuredInTeam(teamId: TeamID): Fu[List[Swiss]] =
    cache.featuredInTeamCache.get(teamId) flatMap { ids =>
      colls.swiss.byOrderedIds[Swiss, Swiss.Id](ids)(_.id)
    }

  def visibleInTeam(teamId: TeamID, nb: Int): Fu[List[Swiss]] =
    colls.swiss.ext.find($doc("teamId" -> teamId)).sort($sort desc "startsAt").list[Swiss](nb)

  def playerInfo(swiss: Swiss, userId: User.ID): Fu[Option[SwissPlayer.ViewExt]] =
    userRepo named userId flatMap {
      _ ?? { user =>
        colls.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, user.id).value) flatMap {
          _ ?? { player =>
            SwissPairing.fields { f =>
              colls.pairing.ext
                .find($doc(f.swissId -> swiss.id, f.players -> player.number))
                .sort($sort asc f.round)
                .list[SwissPairing]()
            } flatMap {
              pairingViews(_, player)
            } flatMap { pairings =>
              SwissPlayer.fields { f =>
                colls.player.countSel($doc(f.swissId -> swiss.id, f.score $gt player.score)).dmap(1.+)
              } map { rank =>
                val pairingMap = pairings.view.map { p =>
                  p.pairing.round -> p
                }.toMap
                SwissPlayer
                  .ViewExt(
                    player,
                    rank,
                    user.light,
                    pairingMap,
                    SwissSheet.one(swiss, pairingMap.view.mapValues(_.pairing).toMap, player)
                  )
                  .some
              }
            }
          }
        }
      }
    }

  def pairingViews(pairings: Seq[SwissPairing], player: SwissPlayer): Fu[Seq[SwissPairing.View]] =
    pairings.headOption ?? { first =>
      SwissPlayer.fields { f =>
        colls.player.ext
          .find($doc(f.swissId -> first.swissId, f.number $in pairings.map(_ opponentOf player.number)))
          .list[SwissPlayer]()
      } flatMap { opponents =>
        lightUserApi asyncMany opponents.map(_.userId) map { users =>
          opponents.zip(users) map {
            case (o, u) => SwissPlayer.WithUser(o, u | LightUser.fallback(o.userId))
          }
        } map { opponents =>
          pairings flatMap { pairing =>
            opponents.find(_.player.number == pairing.opponentOf(player.number)) map {
              SwissPairing.View(pairing, _)
            }
          }
        }
      }
    }

  private[swiss] def finishGame(game: Game): Funit =
    game.swissId ?? { swissId =>
      Sequencing(Swiss.Id(swissId))(startedById) { swiss =>
        colls.pairing.byId[SwissPairing](game.id).dmap(_.filter(_.isOngoing)) flatMap {
          _ ?? { pairing =>
            val winner = game.winnerColor
              .map(_.fold(pairing.white, pairing.black))
              .flatMap(playerNumberHandler.writeOpt)
            colls.pairing.updateField($id(game.id), SwissPairing.Fields.status, winner | BSONNull).void >>
              colls.swiss.update.one($id(swiss.id), $inc("nbOngoing" -> -1)) >>
              scoring.recompute(swiss) >>
              game.playerWhoDidNotMove.flatMap(_.userId).?? { absent =>
                SwissPlayer.fields { f =>
                  colls.player
                    .updateField($doc(f.swissId -> swiss.id, f.userId -> absent), f.absent, true)
                    .void
                }
              } >> {
              (swiss.nbOngoing == 1) ?? {
                if (swiss.round.value == swiss.settings.nbRounds) doFinish(swiss)
                else
                  colls.swiss
                    .updateField(
                      $id(swiss.id),
                      "nextRoundAt",
                      DateTime.now.plusSeconds(swiss.settings.roundInterval.toSeconds.toInt)
                    )
                    .void >>-
                    systemChat(swiss.id, s"Round ${swiss.round.value + 1} will start soon.")
              }
            } >>- socket.reload(swiss.id)
          }
        }
      }
    }

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
    SwissPlayer
      .fields { f =>
        colls.player.ext.find($doc(f.swissId -> swiss.id)).sort($sort desc f.score).one[SwissPlayer]
      }
      .flatMap { winner =>
        colls.swiss.update
          .one(
            $id(swiss.id),
            $unset("nextRoundAt") ++ $set(
              "settings.nbRounds" -> swiss.round,
              "finishedAt"        -> DateTime.now,
              "winnerId"          -> winner.map(_.userId)
            )
          )
          .void
      } >>- {
      socket.reload(swiss.id)
      systemChat(swiss.id, s"Tournament completed!")
    }

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
                  _.fold {
                    systemChat(swiss.id, "All possible pairings were played.")
                    doFinish(swiss)
                  } {
                    case s if s.nextRoundAt.isEmpty =>
                      scoring.recompute(s) >>-
                        systemChat(swiss.id, s"Round ${swiss.round.value + 1} started.")
                    case s =>
                      colls.swiss.update
                        .one($id(swiss.id), $set("nextRoundAt" -> DateTime.now.plusSeconds(61)))
                        .void >>-
                        systemChat(swiss.id, s"Round ${swiss.round.value + 1} failed.", true)
                  }
                }
              else {
                if (swiss.startsAt isBefore DateTime.now.minusMinutes(60)) destroy(swiss)
                else {
                  systemChat(swiss.id, "Not enough players for first round; delaying start.", true)
                  colls.swiss.update
                    .one($id(swiss.id), $set("nextRoundAt" -> DateTime.now.plusSeconds(121)))
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
    } flatMap roundSocket.getGames flatMap { games =>
      val (finished, ongoing) = games.partition(_.finishedOrAborted)
      val flagged             = ongoing.filter(_ outoftime true)
      if (flagged.nonEmpty)
        Bus.publish(lila.hub.actorApi.map.TellMany(flagged.map(_.id), QuietFlag), "roundSocket")
      finished.map(finishGame).sequenceFu.void
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
