package lila.swiss

import chess.{ Black, Color, White }
import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.util.chaining._

import lila.db.dsl._
import lila.game.Game
import lila.user.User

final private class SwissDirector(
    swissColl: Coll @@ SwissColl,
    playerColl: Coll @@ PlayerColl,
    pairingColl: Coll @@ PairingColl,
    pairingSystem: PairingSystem,
    manualPairing: SwissManualPairing,
    gameRepo: lila.game.GameRepo,
    onStart: Game.ID => Unit
)(implicit
    ec: scala.concurrent.ExecutionContext,
    idGenerator: lila.game.IdGenerator
) {
  import BsonHandlers._

  // sequenced by SwissApi
  private[swiss] def startRound(from: Swiss): Fu[Option[Swiss]] =
    (manualPairing(from) | pairingSystem(from))
      .flatMap { pendings =>
        val pendingPairings = pendings.collect { case Right(p) => p }
        if (pendingPairings.isEmpty) fuccess(none) // terminate
        else {
          val swiss = from.startRound
          for {
            players <- SwissPlayer.fields { f =>
              playerColl.list[SwissPlayer]($doc(f.swissId -> swiss.id))
            }
            ids <- idGenerator.games(pendingPairings.size)
            pairings = pendingPairings.zip(ids).map { case (SwissPairing.Pending(w, b), id) =>
              SwissPairing(
                id = id,
                swissId = swiss.id,
                round = swiss.round,
                white = w,
                black = b,
                status = Left(SwissPairing.Ongoing)
              )
            }
            _ <-
              swissColl.update
                .one(
                  $id(swiss.id),
                  $unset("nextRoundAt", "settings.mp") ++ $set(
                    "round"       -> swiss.round,
                    "nbOngoing"   -> pairings.size,
                    "lastRoundAt" -> DateTime.now
                  )
                )
                .void
            date = DateTime.now
            byes = pendings.collect { case Left(bye) => bye.player }
            _ <- SwissPlayer.fields { f =>
              playerColl.update
                .one(
                  $doc(f.userId $in byes, f.swissId -> swiss.id),
                  $addToSet(f.byes                  -> swiss.round),
                  multi = true
                )
                .void
            }
            _ <- pairingColl.insert.many(pairings).void
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
        chess = chess
          .Game(
            variantOption = Some {
              if (swiss.settings.position.isEmpty) swiss.variant
              else chess.variant.FromPosition
            },
            fen = swiss.settings.position
          )
          .copy(clock = swiss.clock.toClock.some),
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
