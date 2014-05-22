package lila

package object chat extends PackageObject with WithPlay {

  private[chat] type ChatId = String

  private[chat] val systemUserId = "lichess"
}
