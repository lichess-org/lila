package lila.challenge

import shogi.{ Color, Mode }
import lila.game.{ Game, Player, Pov, Source }
import lila.user.User

final private class Joiner(
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    onStart: lila.round.OnStart
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(c: Challenge, destUser: Option[User], color: Option[Color]): Fu[Option[Pov]] =
    gameRepo exists c.id flatMap {
      case true                                                           => fuccess(None)
      case _ if color.map(Challenge.ColorChoice.apply).has(c.colorChoice) => fuccess(None)
      case _ =>
        c.challengerUserId.??(userRepo.byId) flatMap { challengerUser =>
          val shogiGame =
            shogi
              .Game(c.initialSfen, c.variant)
              .withClock(c.clock.map(_.config.toClock))
          val perfPicker = (perfs: lila.user.Perfs) => perfs(c.perfType)
          val game = Game
            .make(
              shogi = shogiGame,
              initialSfen = c.initialSfen,
              sentePlayer = Player.make(shogi.Sente, c.finalColor.fold(challengerUser, destUser), perfPicker),
              gotePlayer = Player.make(shogi.Gote, c.finalColor.fold(destUser, challengerUser), perfPicker),
              mode = if (c.initialSfen.isDefined) Mode.Casual else c.mode,
              source = Source.Friend,
              daysPerTurn = c.daysPerTurn,
              notationImport = None
            )
            .withId(c.id)
            .start
          (gameRepo insertDenormalized game) >>- onStart(game.id) inject Pov(game, !c.finalColor).some
        }
    }

}
