package lila.chat

private[chat] object actorApi {

case class Command(chanOption: Option[Chan], member: ChatMember, text: String)

case class Tell(uid: String, line: Line)

case class SetOpen(member: ChatMember, value: Boolean)

case class Query(member: ChatMember, username: String)
}
