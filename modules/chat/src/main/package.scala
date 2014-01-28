package lila

package object chat extends PackageObject with WithPlay {

  private[chat] type ChatId = String

  object tube {

    // implicit lazy val chatTube = Chat.tube inColl Env.current.chatColl
  }
}
