package lila.swiss

import chess.{ Black, Centis, Color, White }
import org.joda.time.DateTime
import scala.util.chaining._

import lila.db.dsl._
import lila.game.Game

final private class SwissDirector(
    colls: SwissColls,
    pairingSystem: PairingSystem,
    gameRepo: lila.game.GameRepo,
    onStart: Game.ID => Unit
)(implicit
    ec: scala.concurrent.ExecutionContext,
    idGenerator: lila.game.IdGenerator
) {
  import BsonHandlers._

  // sequenced by SwissApi
  private[swiss] def startRound(from: Swiss): Fu[Option[Swiss]] =
    fetchPlayers(from)
      .zip(fetchPrevPairings(from))
      .flatMap {
        case (players, prevPairings) =>
          val pendings = pairingSystem(from, players, prevPairings)
          if (pendings.isEmpty) fuccess(none[Swiss]) // terminate
          else {
            val swiss = from.startRound
            for {
              pairings <- pendings.collect {
                case Right(SwissPairing.Pending(w, b)) =>
                  idGenerator.game dmap { id =>
                    SwissPairing(
                      id = id,
                      swissId = swiss.id,
                      round = swiss.round,
                      white = w,
                      black = b,
                      status = Left(SwissPairing.Ongoing)
                    )
                  }
              }.sequenceFu
              _ <-
                colls.swiss.update
                  .one(
                    $id(swiss.id),
                    $unset("nextRoundAt") ++ $set(
                      "round"     -> swiss.round,
                      "nbOngoing" -> pairings.size
                    )
                  )
                  .void
              date = DateTime.now
              byes = pendings.collect { case Left(bye) => bye.player }
              _ <- SwissPlayer.fields { f =>
                colls.player.update
                  .one($doc(f.number $in byes, f.swissId -> swiss.id), $addToSet(f.byes -> swiss.round))
                  .void
              }
              pairingsBson = pairings.map { p =>
                pairingHandler.write(p) ++ $doc(SwissPairing.Fields.date -> date)
              }
              _ <- colls.pairing.insert.many(pairingsBson).void
              playerMap = SwissPlayer.toMap(players)
              games     = pairings.map(makeGame(swiss, playerMap))
              _ <- lila.common.Future.applySequentially(games) { game =>
                gameRepo.insertDenormalized(game) >>- onStart(game.id)
              }
            } yield swiss.some
          }
      }
      .recover {
        case PairingSystem.BBPairingException(msg, input) =>
          logger.warn(s"BBPairing ${from.id} $msg")
          logger.info(s"BBPairing ${from.id} $input")
          from.some
      }
      .monSuccess(_.swiss.startRound)

  private def fetchPlayers(swiss: Swiss) =
    SwissPlayer.fields { f =>
      colls.player.ext
        .find($doc(f.swissId -> swiss.id))
        .sort($sort asc f.number)
        .list[SwissPlayer]()
    }

  private def fetchPrevPairings(swiss: Swiss) =
    SwissPairing.fields { f =>
      colls.pairing.ext
        .find($doc(f.swissId -> swiss.id))
        .sort($sort asc f.round)
        .list[SwissPairing]()
    }

  private def makeGame(swiss: Swiss, players: Map[SwissPlayer.Number, SwissPlayer])(
      pairing: SwissPairing
  ): Game =
    Game
      .make(
        chess = chess.Game(
          variantOption = Some(swiss.variant),
          fen = none
        ) pipe { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = swiss.clock.toClock
              .giveTime(White, Centis(300))
              .giveTime(Black, Centis(300))
              .start
              .some,
            turns = turns,
            startedAtTurn = turns
          )
        },
        whitePlayer = makePlayer(White, players get pairing.white err s"Missing pairing white $pairing"),
        blackPlayer = makePlayer(Black, players get pairing.black err s"Missing pairing black $pairing"),
        mode = chess.Mode(swiss.settings.rated),
        source = lila.game.Source.Swiss,
        pgnImport = None
      )
      .withId(pairing.gameId)
      .withSwissId(swiss.id.value)
      .start

  private def makePlayer(color: Color, player: SwissPlayer) =
    lila.game.Player.make(color, player.userId, player.rating, player.provisional)
}
