package lila.mod

import lila.chat.UserChat
import lila.core.swiss.IdName as Swiss
import lila.core.tournament.Tournament
import lila.report.Suspect
import lila.user.UserRepo

final class PublicChat(
    chatApi: lila.chat.ChatApi,
    tournamentApi: lila.core.tournament.TournamentApi,
    swissFeature: lila.core.swiss.SwissFeatureApi,
    userRepo: UserRepo
)(using Executor):

  def all: Fu[(List[(Tournament, UserChat)], List[(Swiss, UserChat)])] =
    tournamentChats.zip(swissChats)

  def deleteAll(userId: UserId): Funit =
    userRepo.byId(userId).map2(Suspect.apply).flatMapz(deleteAll)

  def deleteAll(suspect: Suspect): Funit =
    all.flatMap: (tours, swisses) =>
      (tours.map(_._2) ::: swisses.map(_._2))
        .filter(_.hasLinesOf(suspect.user))
        .parallelVoid(chatApi.userChat.delete(_, suspect.user, _.global))

  private def tournamentChats: Fu[List[(Tournament, UserChat)]] =
    tournamentApi.fetchModable.flatMap: tours =>
      val ids = tours.map(_.id.into(ChatId))
      chatApi.userChat
        .findAll(ids)
        .map: chats =>
          sortTournamentsByRelevance:
            chats
              .map(_.filterLines(!_.isLichess))
              .filter(_.nonEmpty)
              .flatMap: chat =>
                tours
                  .find(_.id.value == chat.id.value)
                  .map(_ -> chat)

  private def swissChats: Fu[List[(Swiss, UserChat)]] =
    swissFeature.idNames
      .flatMap: swisses =>
        val all = swisses.created ::: swisses.started
        val ids = all.map(_.id.into(ChatId))
        chatApi.userChat
          .findAll(ids)
          .map: chats =>
            chats.flatMap: chat =>
              all.find(_.id.value == chat.id.value).map(_ -> chat)

  // Sort the tournaments by the tournaments most likely to require moderation attention
  private def sortTournamentsByRelevance(tournaments: List[(Tournament, UserChat)]) =
    tournaments.sortBy: t =>
      (t._1.isFinished, -t._1.nbPlayers)
