package lila.chat

import scala.concurrent.duration._
import scala.concurrent.Future

import lila.game.{ GameRepo, Game }
import lila.memo.AsyncCache
import lila.tournament.TournamentRepo
import lila.user.User

private[chat] final class Namer(getUsername: String ⇒ Fu[String]) {

  def chan(c: Chan, as: User): Fu[NamedChan] =
    chanCache(c -> as) map { NamedChan(c, _) }

  def chans(cs: List[Chan], as: User): Fu[List[NamedChan]] =
    Future.traverse(cs) { c ⇒ chan(c, as) }

  private val chanCache = AsyncCache(nameChan, timeToLive = 30 minutes)

  private def nameChan(data: (Chan, User)): Fu[String] = data match {

    case (static: StaticChan, _) ⇒ fuccess(static.name)

    case (c@GameWatcherChan(id), _) ⇒
      GameRepo game id flatten s"No game for chan $c" flatMap nameWatcherChan

    case (c@GamePlayerChan(id), user) ⇒
      GameRepo game id flatten s"No game for chan $c" flatMap { game ⇒
        (game player user).fold(nameWatcherChan(game)) { player ⇒
          lila.game.Namer.player(game opponent player, false)(getUsername) map { opponent ⇒
            s"Game: $opponent"
          }
        }
      }

    case (c@TournamentChan(id), _) ⇒
      TournamentRepo nameById id flatten s"No tournament for chan $c" map {
        _ + " tournament"
      }

    case (c@UserChan(u1, u2), user) ⇒
      if (user.id == u1) getUsername(u2)
      else if (user.id == u2) getUsername(u1)
      else fufail(s"${user.id} can't see $c")

    case (LangChan(lang), _) ⇒ fuccess {
      (lila.i18n.LangList name lang) | lang
    }

    case (c, _) ⇒ fuccess(c.toString)
  }

  private def nameWatcherChan(game: Game) =
    lila.game.Namer.players(game, false)(getUsername) map { case (p1, p2) ⇒ s"$p1 vs $p2" }
}
