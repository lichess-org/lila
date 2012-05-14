package lila
package ai

import com.mongodb.casbah.MongoCollection

import core.Settings

final class AiEnv(
  settings: Settings) {

  import settings._

  val ai: () ⇒ Ai = AiChoice match {
    case AiRemote ⇒ () ⇒ remoteAi or craftyAi
    case AiCrafty ⇒ () ⇒ craftyAi
    case _        ⇒ () ⇒ stupidAi
  }

  lazy val remoteAi = new RemoteAi(remoteUrl = AiRemoteUrl)

  lazy val craftyAi = new CraftyAi(server = craftyServer)

  lazy val craftyServer = new CraftyServer(
    execPath = AiCraftyExecPath,
    bookPath = AiCraftyBookPath)

  lazy val stupidAi = new StupidAi

  def isServer = AiServerMode
}
