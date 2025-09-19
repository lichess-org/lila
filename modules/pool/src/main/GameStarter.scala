package lila.pool

import chess.ByColor

import lila.core.game.{ GameRepo, IdGenerator, NewPlayer, Source }
import lila.core.pool.{ Pairing, Pairings }
import lila.common.Bus

final private class GameStarter(
    userApi: lila.core.user.UserApi,
    gameRepo: GameRepo,
    newPlayer: NewPlayer,
    idGenerator: IdGenerator,
    onStart: GameId => Unit
)(using Executor, Scheduler):

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 10.seconds,
    name = "gameStarter",
    lila.log.asyncActorMonitor.full
  )

  def apply(pool: PoolConfig, couples: Vector[MatchMaking.Couple]): Funit =
    couples.nonEmpty.so:
      val userIds = couples.flatMap(_.userIds)
      workQueue:
        for
          (perfs, ids) <- userApi.perfOf(userIds, pool.perfKey).zip(idGenerator.games(couples.size))
          pairingOpts <- couples.zip(ids).parallel(one(pool, perfs).tupled)
        yield
          val pairings = pairingOpts.flatten.toList
          for
            pairing <- pairings
            (sri, _) <- pairing.players.toList
          do Bus.publishDyn(pairing, s"hookRemove:$sri")
          Bus.pub(Pairings(pairings))

  private def one(pool: PoolConfig, perfs: Map[UserId, Perf])(
      couple: MatchMaking.Couple,
      id: GameId
  ): Fu[Option[Pairing]] =
    import couple.*
    (perfs.get(p1.userId), perfs.get(p2.userId)).tupled.traverse: (perf1, perf2) =>
      for
        p1White <- userApi.firstGetsWhite(p1.userId, p2.userId)
        (whitePerf, blackPerf) = if p1White then perf1 -> perf2 else perf2 -> perf1
        (whiteMember, blackMember) = if p1White then p1 -> p2 else p2 -> p1
        game = makeGame(
          id,
          pool,
          whiteMember.userId -> whitePerf,
          blackMember.userId -> blackPerf
        ).start
        _ <- gameRepo.insertDenormalized(game)
      yield
        onStart(game.id)
        Pairing(ByColor(whiteMember.sri -> game.fullIds.white, blackMember.sri -> game.fullIds.black))

  private def makeGame(
      id: GameId,
      pool: PoolConfig,
      whiteUser: (UserId, Perf),
      blackUser: (UserId, Perf)
  ) =
    Game(
      id = id,
      chess = chess.Game(
        position = chess.variant.Standard.initialPosition,
        clock = pool.clock.toClock.some
      ),
      players = ByColor(whiteUser, blackUser).mapWithColor((u, p) => newPlayer(u, p)),
      rated = chess.Rated.Yes,
      status = chess.Status.Created,
      daysPerTurn = none,
      metadata = lila.core.game.newMetadata(Source.Pool)
    )
