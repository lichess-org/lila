package lila.mod

import lila.chat.{ Chat, UserChat }
import lila.report.Suspect
import lila.swiss.Swiss
import lila.tournament.Tournament
import lila.user.{ User, UserRepo }

final class PublicChat(
    chatApi: lila.chat.ChatApi,
    tournamentApi: lila.tournament.TournamentApi,
    swissFeature: lila.swiss.SwissFeature,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def all: Fu[(List[(Tournament, UserChat)], List[(Swiss, UserChat)])] =
    tournamentChats zip swissChats

  def deleteAll(userId: User.ID): Funit =
    userRepo byId userId map2 Suspect flatMap { _ ?? deleteAll }

  def deleteAll(suspect: Suspect): Funit =
    all.flatMap { case (tours, simuls) =>
      (tours.map(_._2) ::: simuls.map(_._2))
        .filter(_ hasLinesOf suspect.user)
        .map(chatApi.userChat.delete(_, suspect.user, _.Global))
        .sequenceFu
        .void
    }

  private def tournamentChats: Fu[List[(Tournament, UserChat)]] =
    tournamentApi.fetchVisibleTournaments.flatMap { visibleTournaments =>
      val ids = visibleTournaments.all.map(_.id) map Chat.Id
      chatApi.userChat.findAll(ids).map { chats =>
        chats.flatMap { chat =>
          visibleTournaments.all.find(_.id == chat.id.value).map(_ -> chat)
        }
      } map sortTournamentsByRelevance
    }

  private def swissChats: Fu[List[(Swiss, UserChat)]] =
    swissFeature.get(Nil).flatMap { swisses =>
      val all = swisses.created ::: swisses.started
      val ids = all.map(_.id.value) map Chat.Id
      chatApi.userChat.findAll(ids).map { chats =>
        chats.flatMap { chat =>
          all.find(_.id.value == chat.id.value).map(_ -> chat)
        }
      }
    }

  /** Sort the tournaments by the tournaments most likely to require moderation attention
    */
  private def sortTournamentsByRelevance(tournaments: List[(Tournament, UserChat)]) =
    tournaments.sortBy { t =>
      (t._1.isFinished, -t._1.nbPlayers)
    }
}
