package lila.swiss

import chess.ByColor
import monocle.syntax.all.*

import lila.db.dsl.{ *, given }

final private class SwissDirector(
    mongo: SwissMongo,
    pairingSystem: PairingSystem,
    manualPairing: SwissManualPairing,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    onStart: lila.core.game.OnStart
)(using
    ec: Executor,
    idGenerator: lila.core.game.IdGenerator
):
  import BsonHandlers.given

  // sequenced by SwissApi
  private[swiss] def startRound(from: Swiss): Fu[Option[Swiss]] =
    (manualPairing(from) | pairingSystem(from))
      .flatMap { pendings =>
        val pendingPairings = pendings.collect { case Right(p) => p }
        if pendingPairings.isEmpty then fuccess(none) // terminate
        else
          val swiss = from.startRound
          for
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
                  $doc(f.userId.$in(byes), f.swissId -> swiss.id),
                  $addToSet(f.byes                   -> swiss.round),
                  multi = true
                )
                .void
            }
            _ <- mongo.pairing.insert.many(pairings).void
            games = pairings.map(makeGame(swiss, players.mapBy(_.userId)))
            _ <- games.sequentiallyVoid: game =>
              for _ <- gameRepo.insertDenormalized(game) yield onStart.exec(game.id)
          yield swiss.some
      }
      .recover { case PairingSystem.BBPairingException(msg, input) =>
        if msg.contains("The number of rounds is larger than the reported number of rounds.") then none
        else
          logger.warn(s"BBPairing ${from.id} $msg")
          logger.info(s"BBPairing ${from.id} $input")
          from.some
      }
      .monSuccess(_.swiss.startRound)

  private def makeGame(swiss: Swiss, players: Map[UserId, SwissPlayer])(pairing: SwissPairing): Game =
    lila.core.game
      .newGame(
        chess = chess
          .Game(
            variantOption = Some {
              if swiss.settings.position.isEmpty then swiss.variant
              else chess.variant.FromPosition
            },
            fen = swiss.settings.position
          )
          .copy(clock = swiss.clock.toClock.some),
        players = ByColor: c =>
          val player = players.get(pairing(c)).err(s"Missing pairing $c $pairing")
          newPlayer(c, player.userId, player.rating, player.provisional)
        ,
        mode = chess.Mode(swiss.settings.rated),
        source = lila.core.game.Source.Swiss,
        pgnImport = None
      )
      .withId(pairing.gameId)
      .focus(_.metadata.swissId)
      .replace(swiss.id.some)
      .start
