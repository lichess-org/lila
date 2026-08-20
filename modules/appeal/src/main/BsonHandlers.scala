package lila.appeal

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

private object BsonHandlers:

  import Appeal.Status
  import AppealMsg.Kind

  given BSONHandler[Status] = stringAnyValHandler(_.toString, t => Status(t) | Status.read)

  given BSONHandler[AppealTopic] =
    stringAnyValHandler(_.key, t => AppealTopic.byKey.getOrElse(t, AppealTopic.legacy))

  given legacyHandler: BSONDocumentHandler[LegacyMessage] = Macros.handler
  given userMessageHandler: BSONDocumentHandler[UserMessageEvent] = Macros.handler
  given modMessageHandler: BSONDocumentHandler[ModMessageEvent] = Macros.handler
  given userChoiceHandler: BSONDocumentHandler[UserChoiceEvent] = Macros.handler
  given modChoiceHandler: BSONDocumentHandler[ModChoiceEvent] = Macros.handler

  given BSONHandler[AppealMsg] = new BSON[AppealMsg]:
    def reads(r: BSON.Reader): AppealMsg =
      r.strO("kind").flatMap(Kind.apply) match
        case None => legacyHandler.readTry(r.doc).get
        case Some(Kind.userMessage) => userMessageHandler.readTry(r.doc).get
        case Some(Kind.modMessage) => modMessageHandler.readTry(r.doc).get
        case Some(Kind.userChoice) => userChoiceHandler.readTry(r.doc).get
        case Some(Kind.modChoice) => modChoiceHandler.readTry(r.doc).get
    def writes(w: BSON.Writer, msg: AppealMsg) =
      val doc = msg match
        case m: LegacyMessage => legacyHandler.writeTry(m).get
        case m: UserMessageEvent => userMessageHandler.writeTry(m).get
        case m: ModMessageEvent => modMessageHandler.writeTry(m).get
        case m: UserChoiceEvent => userChoiceHandler.writeTry(m).get
        case m: ModChoiceEvent => modChoiceHandler.writeTry(m).get
      msg.kind.fold(doc)(k => doc ++ $doc("kind" -> k.key))

  given BSONDocumentHandler[AccountsDisclosure] = Macros.handler
  given BSONDocumentHandler[Appeal] = Macros.handler
