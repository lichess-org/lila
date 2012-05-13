package lila
package ui

import http.Context
import i18n.I18nKeys
import controllers.routes

import play.api.mvc.Call

final class LobbyMenu(i18nKeys: I18nKeys) {

  sealed class Elem(
    val code: String,
    val route: Call,
    val name: I18nKeys#Key,
    val title: I18nKeys#Key)

  val hook = new Elem(
    "hook",
    routes.Lobby.home, 
    i18nKeys.createAGame, 
    i18nKeys.createAGame)

  val friend = new Elem(
    "friend",
    routes.Lobby.home, 
    i18nKeys.playWithAFriend,
    i18nKeys.inviteAFriendToPlayWithYou)

  val ai = new Elem(
    "ai",
    routes.Lobby.home, 
    i18nKeys.playWithTheMachine,
    i18nKeys.challengeTheArtificialIntelligence)

  val all = List(hook, friend, ai)
}
