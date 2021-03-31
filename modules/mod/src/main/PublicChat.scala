package lila.mod

import lila.chat.{ Chat, UserChat }
import lila.report.Suspect
import lila.simul.Simul
import lila.tournament.Tournament
import lila.user.{ User, UserRepo }

final class PublicChat(
    chatApi: lila.chat.ChatApi,
    tournamentApi: lila.tournament.TournamentApi,
    simulEnv: lila.simul.Env,
    userRepo: UserRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def all: Fu[(List[(Tournament, UserChat)], List[(Simul, UserChat)])] =
    tournamentChats zip simulChats

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
      val ids = visibleTournaments.all.map(_.id) map Chat.Id.apply
      chatApi.userChat.findAll(ids).map { chats =>
        chats.flatMap { chat =>
          visibleTournaments.all.find(_.id == chat.id.value).map(tour => (tour, chat))
        }
      } map sortTournamentsByRelevance
    }

  private def simulChats: Fu[List[(Simul, UserChat)]] =
    fetchVisibleSimuls.flatMap { simuls =>
      val ids = simuls.map(_.id) map Chat.Id.apply
      chatApi.userChat.findAll(ids).map { chats =>
        chats.flatMap { chat =>
          simuls.find(_.id == chat.id.value).map(simul => (simul, chat))
        }
      }
    }

  private def fetchVisibleSimuls: Fu[List[Simul]] = {
    simulEnv.allCreatedFeaturable.get {} zip
      simulEnv.repo.allStarted zip
      simulEnv.repo.allFinishedFeaturable(3) map { case ((created, started), finished) =>
        created ::: started ::: finished
      }
  }

  /** Sort the tournaments by the tournaments most likely to require moderation attention
    */
  private def sortTournamentsByRelevance(tournaments: List[(Tournament, UserChat)]) =
    tournaments.sortBy { t =>
      (t._1.isFinished, -t._1.nbPlayers)
    }
}
