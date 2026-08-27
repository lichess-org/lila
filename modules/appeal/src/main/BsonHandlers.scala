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
  given choiceHandler: BSONDocumentHandler[ChoiceEvent] = Macros.handler
  given messageHandler: BSONDocumentHandler[MessageEvent] = Macros.handler
  given actionHandler: BSONDocumentHandler[ActionEvent] = Macros.handler

  given BSONHandler[AppealMsg] = new BSON[AppealMsg]:
    def reads(r: BSON.Reader): AppealMsg =
      r.strO("kind").flatMap(Kind.apply) match
        case None => legacyHandler.readTry(r.doc).get
        case Some(Kind.choice) => choiceHandler.readTry(r.doc).get
        case Some(Kind.message) => messageHandler.readTry(r.doc).get
        case Some(Kind.action) => actionHandler.readTry(r.doc).get
    def writes(w: BSON.Writer, msg: AppealMsg) =
      val doc = msg match
        case m: LegacyMessage => legacyHandler.writeTry(m).get
        case m: MessageEvent => messageHandler.writeTry(m).get
        case m: ChoiceEvent => choiceHandler.writeTry(m).get
        case m: ActionEvent => actionHandler.writeTry(m).get
      msg.kind.fold(doc)(k => doc ++ $doc("kind" -> k.key))

  given BSONDocumentHandler[AccountsDisclosure] = Macros.handler
  given BSONDocumentHandler[Appeal] = Macros.handler
