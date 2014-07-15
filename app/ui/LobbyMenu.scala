package lila.app
package ui

import lila.i18n.I18nKeys
import controllers.routes

import play.api.mvc.Call

private[app] final class LobbyMenu(i18nKeys: I18nKeys) {

  sealed class Elem(
    val code: String,
    val route: Call,
    val name: I18nKeys#Key)

  val hook = new Elem(
    "hook",
    routes.Setup.hookForm, 
    i18nKeys.createAGame)

  val friend = new Elem(
    "friend",
    routes.Setup.friendForm(none), 
    i18nKeys.playWithAFriend)

  val ai = new Elem(
    "ai",
    routes.Setup.aiForm, 
    i18nKeys.playWithTheMachine)

  val all = List(hook, friend, ai)
}
