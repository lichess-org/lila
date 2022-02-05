package lila.swiss

import shogi.{ Color, Gote, Sente }
import org.joda.time.DateTime

import lila.db.dsl._
import lila.game.Game
import lila.user.User

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
    pairingSystem(from)
      .flatMap { pendings =>
        val pendingPairings = pendings.collect { case Right(p) => p }
        if (pendingPairings.isEmpty) fuccess(none) // terminate
        else {
          val swiss = from.startRound
          for {
            players <- SwissPlayer.fields { f =>
              colls.player.list[SwissPlayer]($doc(f.swissId -> swiss.id))
            }
            ids <- idGenerator.games(pendingPairings.size)
            pairings = pendingPairings.zip(ids).map { case (SwissPairing.Pending(s, g), id) =>
              SwissPairing(
                id = id,
                swissId = swiss.id,
                round = swiss.round,
                sente = s,
                gote = g,
                status = Left(SwissPairing.Ongoing)
              )
            }
            _ <-
              colls.swiss.update
                .one(
                  $id(swiss.id),
                  $unset("nextRoundAt") ++ $set(
                    "round"       -> swiss.round,
                    "nbOngoing"   -> pairings.size,
                    "lastRoundAt" -> DateTime.now
                  )
                )
                .void
            date = DateTime.now
            byes = pendings.collect { case Left(bye) => bye.player }
            _ <- SwissPlayer.fields { f =>
              colls.player.update
                .one($doc(f.userId $in byes, f.swissId -> swiss.id), $addToSet(f.byes -> swiss.round))
                .void
            }
            _ <- colls.pairing.insert.many(pairings).void
            games = pairings.map(makeGame(swiss, SwissPlayer.toMap(players)))
            _ <- lila.common.Future.applySequentially(games) { game =>
              gameRepo.insertDenormalized(game) >>- onStart(game.id)
            }
          } yield swiss.some
        }
      }
      .recover { case PairingSystem.BBPairingException(msg, input) =>
        if (msg contains "The number of rounds is larger than the reported number of rounds.") none
        else {
          logger.warn(s"BBPairing ${from.id} $msg")
          logger.info(s"BBPairing ${from.id} $input")
          from.some
        }
      }
      .monSuccess(_.swiss.startRound)

  private def makeGame(swiss: Swiss, players: Map[User.ID, SwissPlayer])(
      pairing: SwissPairing
  ): Game =
    Game
      .make(
        shogi = shogi
          .Game(
            variantOption = Some(swiss.variant),
            sfen = none
          )
          .copy(
            clock = swiss.clock.toClock.some
          ),
        sentePlayer = makePlayer(Sente, players get pairing.sente err s"Missing pairing sente $pairing"),
        gotePlayer = makePlayer(Gote, players get pairing.gote err s"Missing pairing gote $pairing"),
        mode = shogi.Mode(swiss.settings.rated),
        source = lila.game.Source.Swiss,
        notationImport = None
      )
      .withId(pairing.gameId)
      .withSwissId(swiss.id.value)
      .start

  private def makePlayer(color: Color, player: SwissPlayer) =
    lila.game.Player.make(color, player.userId, player.rating, player.provisional)
}
