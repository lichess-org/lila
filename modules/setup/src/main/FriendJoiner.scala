package lila.setup

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.{ Color => ChessColor }

import lila.game.{ GameRepo, Game, Pov, Event, Progress, AnonCookie, PerfPicker }
import lila.user.User
import makeTimeout.short

private[setup] final class FriendJoiner(
    friendConfigMemo: FriendConfigMemo,
    onStart: String => Unit) {

  def apply(game: Game, user: Option[User]): Valid[Fu[(Pov, List[Event])]] =
    game.notStarted option {
      val color = (friendConfigMemo get game.id map (!_.creatorColor)) orElse
        // damn, no cache. maybe the opponent was logged, so we can guess?
        Some(ChessColor.Black).ifTrue(game.whitePlayer.hasUser) orElse
        Some(ChessColor.White).ifTrue(game.blackPlayer.hasUser) getOrElse
        ChessColor.Black // well no. we're fucked. toss the coin.
      val g1 = user.fold(game) { u =>
        game.updatePlayer(color, _.withUser(u.id, PerfPicker.mainOrDefault(game)(u.perfs)))
      }
      for {
        _ ← GameRepo.setUsers(g1.id, g1.player(_.white).userInfos, g1.player(_.black).userInfos)
        p1 = Progress(game, g1.start)
        p2 = p1 + Event.RedirectOwner(
          !color,
          p1.game fullIdOf !color,
          AnonCookie.json(p1.game, !color))
        _ ← GameRepo save p2
      } yield {
        onStart(p2.game.id)
        Pov(p2.game, color) -> p2.events
      }
    } toValid "Can't join started game " + game.id
}
