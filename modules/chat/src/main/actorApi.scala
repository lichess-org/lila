package lila.chat

private[chat] object actorApi {

case class Command(chanOption: Option[Chan], member: ChatMember[_], text: String)

case class Tell(uid: String, line: Line)

case class SetOpen(member: ChatMember[_], value: Boolean)

case class Query(member: ChatMember[_], username: String)
}
