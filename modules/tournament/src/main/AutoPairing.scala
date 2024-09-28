package lila.tournament

import shogi.{ Color, Gote, Sente }

import lila.game.{ Game, GameRepo, Player => GamePlayer, Source }
import lila.user.User

final class AutoPairing(
    gameRepo: GameRepo,
    duelStore: DuelStore,
    lightUserApi: lila.user.LightUserApi,
    onStart: Game.ID => Unit
)(implicit ec: scala.concurrent.ExecutionContext, idGenerator: lila.game.IdGenerator) {

  def apply(
      tour: Tournament,
      pairing: Pairing,
      playersMap: Map[User.ID, Player],
      ranking: Ranking
  ): Fu[Game] = {
    val player1 = playersMap get pairing.user1 err s"Missing pairing player1 $pairing"
    val player2 = playersMap get pairing.user2 err s"Missing pairing player2 $pairing"
    val clock   = tour.timeControl.clock.map(_.toClock)
    val game = Game
      .make(
        shogi = shogi
          .Game(
            tour.position,
            tour.variant
          )
          .copy(clock = clock),
        daysPerTurn = tour.timeControl.days,
        initialSfen = tour.position,
        sentePlayer = makePlayer(Sente, player1),
        gotePlayer = makePlayer(Gote, player2),
        mode = tour.mode,
        source = Source.Tournament,
        notationImport = None
      )
      .withId(pairing.gameId)
      .withTournamentId(tour.id)
      .start
    (gameRepo insertDenormalized game) >>- {
      onStart(game.id)
      duelStore.add(
        tour = tour,
        game = game,
        p1 = (usernameOf(pairing.user1) -> ~game.sentePlayer.rating),
        p2 = (usernameOf(pairing.user2) -> ~game.gotePlayer.rating),
        ranking = ranking
      )
    } inject game
  }

  def apply(
      tour: Tournament,
      arrangement: Arrangement,
      playersMap: Map[User.ID, Player]
  ): Fu[Game] = {
    val player1        = playersMap get arrangement.user1.id err s"Missing arrangement player1 $arrangement"
    val player2        = playersMap get arrangement.user2.id err s"Missing arrangement player2 $arrangement"
    val player1IsSente = ~arrangement.color.map(_.sente) || lila.common.ThreadLocalRandom.nextBoolean()
    val clock          = tour.timeControl.clock.map(_.toClock)
    idGenerator.game flatMap { gid =>
      val game = Game
        .make(
          shogi = shogi
            .Game(
              tour.position,
              tour.variant
            )
            .copy(clock = clock),
          daysPerTurn = tour.timeControl.days,
          initialSfen = tour.position,
          sentePlayer = makePlayer(Sente, if (player1IsSente) player1 else player2),
          gotePlayer = makePlayer(Gote, if (player1IsSente) player2 else player1),
          mode = tour.mode,
          source = Source.Tournament,
          notationImport = None
        )
        .withId(gid)
        .withTournamentId(tour.id)
        .withArrangementId(arrangement.id)
        .start
      (gameRepo insertDenormalized game) >>- {
        onStart(game.id)
      } inject game
    }
  }

  private def makePlayer(color: Color, player: Player) =
    GamePlayer.make(color, player.userId, player.rating, provisional = player.provisional, isBot = false, hasTitle = false)

  private def usernameOf(userId: User.ID) =
    lightUserApi.sync(userId).fold(userId)(_.name)
}
