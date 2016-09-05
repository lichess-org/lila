package lila.mod

import lila.chat.UserChat
import lila.simul.{ Simul => SimulModel }
import lila.tournament.{ Tournament => TournamentModel }

final class PublicChat(
    chatApi: lila.chat.ChatApi,
    tournamentApi: lila.tournament.TournamentApi,
    simulEnv: lila.simul.Env) {

  def tournamentChats: Fu[List[(TournamentModel, UserChat)]] =
    tournamentApi.fetchVisibleTournaments.flatMap {
      visibleTournaments =>
        val tournamentList = sortTournamentsByRelevance(visibleTournaments.all)
        val ids = tournamentList.map(_.id)
        chatApi.userChat.findAll(ids).map {
          chats =>
            chats.map { chat =>
              tournamentList.find(_.id === chat.id).map(tour => (tour, chat))
            }.flatten
        }
    }

  def simulChats: Fu[List[(SimulModel, UserChat)]] =
    fetchVisibleSimuls.flatMap {
      simuls =>
        var ids = simuls.map(_.id)
        chatApi.userChat.findAll(ids).map {
          chats =>
            chats.map { chat =>
              simuls.find(_.id === chat.id).map(simul => (simul, chat))
            }.flatten
        }
    }

  private def fetchVisibleSimuls: Fu[List[SimulModel]] = {
    simulEnv.allCreated(true) zip
      simulEnv.repo.allStarted zip
      simulEnv.repo.allFinished(5) map {
        case ((created, started), finished) =>
          created ::: started ::: finished
      }
  }

  /**
   * Sort the tournaments by the tournaments most likely to require moderation attention
   */
  private def sortTournamentsByRelevance(tournaments: List[TournamentModel]): List[TournamentModel] =
    tournaments.sortBy(-_.nbPlayers)
}
