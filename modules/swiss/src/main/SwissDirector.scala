package lila.swiss

import chess.{ Black, Color, White }

import lila.db.dsl.{ *, given }
import lila.game.Game

final private class SwissDirector(
    mongo: SwissMongo,
    pairingSystem: PairingSystem,
    manualPairing: SwissManualPairing,
    gameRepo: lila.game.GameRepo,
    onStart: lila.round.OnStart
)(using
    ec: Executor,
    idGenerator: lila.game.IdGenerator
):
  import BsonHandlers.given

  // sequenced by SwissApi
  private[swiss] def startRound(from: Swiss): Fu[Option[Swiss]] =
    (manualPairing(from) | pairingSystem(from))
      .flatMap { pendings =>
        val pendingPairings = pendings.collect { case Right(p) => p }
        if (pendingPairings.isEmpty) fuccess(none) // terminate
        else
          val swiss = from.startRound
          for {
            players <- SwissPlayer.fields { f =>
              mongo.player.list[SwissPlayer]($doc(f.swissId -> swiss.id))
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
              mongo.swiss.update
                .one(
                  $id(swiss.id),
                  $unset("nextRoundAt", "settings.mp") ++ $set(
                    "round"       -> swiss.round,
                    "nbOngoing"   -> pairings.size,
                    "lastRoundAt" -> nowInstant
                  )
                )
                .void
            date = nowInstant
            byes = pendings.collect { case Left(bye) => bye.player }
            _ <- SwissPlayer.fields { f =>
              mongo.player.update
                .one(
                  $doc(f.userId $in byes, f.swissId -> swiss.id),
                  $addToSet(f.byes                  -> swiss.round),
                  multi = true
                )
                .void
            }
            _ <- mongo.pairing.insert.many(pairings).void
            games = pairings.map(makeGame(swiss, players.mapBy(_.userId)))
            _ <- lila.common.LilaFuture.applySequentially(games) { game =>
              gameRepo.insertDenormalized(game) >>- onStart(game.id)
            }
          } yield swiss.some
      }
      .recover { case PairingSystem.BBPairingException(msg, input) =>
        if (msg contains "The number of rounds is larger than the reported number of rounds.") none
        else
          logger.warn(s"BBPairing ${from.id} $msg")
          logger.info(s"BBPairing ${from.id} $input")
          from.some
      }
      .monSuccess(_.swiss.startRound)

  private def makeGame(swiss: Swiss, players: Map[UserId, SwissPlayer])(pairing: SwissPairing): Game =
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
      .withSwissId(swiss.id)
      .start

  private def makePlayer(color: Color, player: SwissPlayer) =
    lila.game.Player.make(color, player.userId, player.rating, player.provisional)
