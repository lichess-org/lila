package lila.appeal

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

private object BsonHandlers:

  import Appeal.Status

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
      r.strO("kind") match
        case None => legacyHandler.readTry(r.doc).get
        case Some("userMessage") => userMessageHandler.readTry(r.doc).get
        case Some("modMessage") => modMessageHandler.readTry(r.doc).get
        case Some("userChoice") => userChoiceHandler.readTry(r.doc).get
        case Some("modChoice") => modChoiceHandler.readTry(r.doc).get
        case Some(other) => sys.error(s"unknown appeal msg kind: $other")
    def writes(w: BSON.Writer, msg: AppealMsg) =
      val doc = msg match
        case m: LegacyMessage => legacyHandler.writeTry(m).get
        case m: UserMessageEvent => userMessageHandler.writeTry(m).get
        case m: ModMessageEvent => modMessageHandler.writeTry(m).get
        case m: UserChoiceEvent => userChoiceHandler.writeTry(m).get
        case m: ModChoiceEvent => modChoiceHandler.writeTry(m).get
      msg.kind.fold(doc)(k => doc ++ $doc("kind" -> k))

  given BSONDocumentHandler[AccountsDisclosure] = Macros.handler
  given BSONDocumentHandler[Appeal] = Macros.handler
