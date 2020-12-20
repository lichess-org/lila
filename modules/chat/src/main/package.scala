package lila

package object chat extends PackageObject {

  private[chat] def logger = lila.log("chat")

  private[chat] val systemUserId = "lichess"
}
