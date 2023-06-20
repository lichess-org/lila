package lila.mod

import lila.chat.UserChat
import lila.report.Suspect
import lila.swiss.Swiss
import lila.tournament.Tournament
import lila.user.UserRepo

final class PublicChat(
    chatApi: lila.chat.ChatApi,
    tournamentApi: lila.tournament.TournamentApi,
    swissFeature: lila.swiss.SwissFeature,
    userRepo: UserRepo
)(using Executor):

  def all: Fu[(List[(Tournament, UserChat)], List[(Swiss, UserChat)])] =
    tournamentChats zip swissChats

  def deleteAll(userId: UserId): Funit =
    userRepo byId userId map2 Suspect.apply flatMapz deleteAll

  def deleteAll(suspect: Suspect): Funit =
    all.flatMap { case (tours, swisses) =>
      (tours.map(_._2) ::: swisses.map(_._2))
        .filter(_ hasLinesOf suspect.user)
        .map(chatApi.userChat.delete(_, suspect.user, _.Global))
        .parallel
        .void
    }

  private def tournamentChats: Fu[List[(Tournament, UserChat)]] =
    tournamentApi.fetchVisibleTournaments.flatMap { visibleTournaments =>
      val ids = visibleTournaments.all.map(_.id into ChatId)
      chatApi.userChat.findAll(ids).map { chats =>
        chats.flatMap { chat =>
          visibleTournaments.all.find(_.id.value == chat.id.value).map(_ -> chat)
        }
      } map sortTournamentsByRelevance
    }

  private def swissChats: Fu[List[(Swiss, UserChat)]] =
    swissFeature.get(Nil).flatMap { swisses =>
      val all = swisses.created ::: swisses.started
      val ids = all.map(_.id into ChatId)
      chatApi.userChat.findAll(ids).map { chats =>
        chats.flatMap { chat =>
          all.find(_.id.value == chat.id.value).map(_ -> chat)
        }
      }
    }

  // Sort the tournaments by the tournaments most likely to require moderation attention
  private def sortTournamentsByRelevance(tournaments: List[(Tournament, UserChat)]) =
    tournaments.sortBy { t =>
      (t._1.isFinished, -t._1.nbPlayers)
    }
