package lila.swiss

import chess.{ Black, Color, White }
import org.joda.time.DateTime
import scala.util.chaining._

import lila.db.dsl._
import lila.game.Game

final private class SwissDirector(
    colls: SwissColls,
    pairingSystem: PairingSystem,
    gameRepo: lila.game.GameRepo,
    onStart: Game.ID => Unit
)(
    implicit ec: scala.concurrent.ExecutionContext,
    idGenerator: lila.game.IdGenerator
) {
  import BsonHandlers._

  // sequenced by SwissApi
  private[swiss] def startRound(from: Swiss): Funit =
    for {
      players      <- fetchPlayers(from)
      prevPairings <- fetchPrevPairings(from)
      swiss = from.startRound
      _ <- colls.swiss.update.one($id(swiss.id), $set("round" -> swiss.round) ++ $unset("nextRoundAt")).void
      pairings <- pairingSystem(swiss, players, prevPairings).map {
        case SwissPairing.Pending(w, b) =>
          idGenerator.game map { id =>
            SwissPairing(
              _id = id,
              swissId = swiss.id,
              round = swiss.round,
              white = w,
              black = b,
              status = Left(SwissPairing.Ongoing)
            )
          }
      }.sequenceFu
      date = DateTime.now
      pairingsBson = pairings.map { p =>
        pairingHandler.write(p) ++ $doc(SwissPairing.Fields.date -> date)
      }
      _ <- colls.pairing.insert.many(pairingsBson).void
      playerMap = players.map(p => p.number -> p).toMap
      games     = pairings.map(makeGame(swiss, playerMap))
      _ <- lila.common.Future.applySequentially(games) { game =>
        gameRepo.insertDenormalized(game) >>- onStart(game.id)
      }
    } yield ()

  private def fetchPlayers(swiss: Swiss) = SwissPlayer.fields { f =>
    colls.player.ext
      .find($doc(f.swissId -> swiss.id))
      .sort($sort asc f.number)
      .list[SwissPlayer]()
  }

  private def fetchPrevPairings(swiss: Swiss) = SwissPairing.fields { f =>
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
            clock = swiss.clock.toClock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
        whitePlayer = makePlayer(White, players get pairing.white err s"Missing pairing white $pairing"),
        blackPlayer = makePlayer(Black, players get pairing.black err s"Missing pairing black $pairing"),
        mode = chess.Mode(swiss.rated),
        source = lila.game.Source.Swiss,
        pgnImport = None
      )
      .withId(pairing.gameId)
      .withSwissId(swiss.id.value)
      .start

  private def makePlayer(color: Color, player: SwissPlayer) =
    lila.game.Player.make(color, player.userId, player.rating, player.provisional)
}
