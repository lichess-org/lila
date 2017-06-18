package lila.study

import chess.format.{ Uci, UciCharPair, FEN }
import chess.Pos
import play.api.libs.json._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.socket.Socket.Uid
import lila.tree.Node.Shape
import lila.user.User

final class JsonView(
    studyRepo: StudyRepo,
    lightUser: LightUser.GetterSync
) {

  import JsonView._

  def apply(
    study: Study,
    chapters: List[Chapter.Metadata],
    currentChapter: Chapter,
    me: Option[User]
  ) = {

    def allowed(selection: Settings.UserSelection): Boolean =
      Settings.UserSelection.allows(selection, study, me.map(_.id))

    me.?? { studyRepo.liked(study, _) } map { liked =>
      studyWrites.writes(study) ++ Json.obj(
        "liked" -> liked,
        "features" -> Json.obj(
          "cloneable" -> allowed(study.settings.cloneable),
          "chat" -> allowed(study.settings.chat),
          "sticky" -> study.settings.sticky
        ),
        "chapters" -> chapters.map(chapterMetadataWrites.writes),
        "chapter" -> Json.obj(
          "id" -> currentChapter.id,
          "ownerId" -> currentChapter.ownerId,
          "setup" -> currentChapter.setup,
          "tags" -> currentChapter.tags,
          "practice" -> currentChapter.isPractice,
          "conceal" -> currentChapter.conceal,
          "features" -> Json.obj(
            "computer" -> allowed(study.settings.computer),
            "explorer" -> allowed(study.settings.explorer)
          )
        )
      )
    }
  }.chronometer
    // .mon(_.fishnet.acquire time client.skill.key)
    .logIfSlow(100, logger)(_ => s"JsonView ${study.id} ${study.name}")
    .result

  def chapterConfig(c: Chapter) = Json.obj(
    "id" -> c.id,
    "name" -> c.name,
    "practice" -> c.practice,
    "conceal" -> c.conceal,
    "orientation" -> c.setup.orientation
  )

  private[study] implicit val memberRoleWrites = Writes[StudyMember.Role] { r =>
    JsString(r.id)
  }
  private[study] implicit val memberWrites: Writes[StudyMember] = Writes[StudyMember] { m =>
    Json.obj(
      "user" -> lightUser(m.id),
      "role" -> m.role,
      "addedAt" -> m.addedAt
    )
  }

  private[study] implicit val membersWrites: Writes[StudyMembers] = Writes[StudyMembers] { m =>
    Json toJson m.members
  }

  private implicit val studyWrites = OWrites[Study] { s =>
    Json.obj(
      "id" -> s.id,
      "name" -> s.name,
      "members" -> s.members,
      "position" -> s.position,
      "ownerId" -> s.ownerId,
      "settings" -> s.settings,
      "visibility" -> s.visibility,
      "createdAt" -> s.createdAt,
      "from" -> s.from,
      "likes" -> s.likes,
      "isNew" -> s.isNew.option(true)
    ).noNull
  }
}

object JsonView {

  case class JsData(study: JsObject, analysis: JsObject)

  implicit val studyIdWrites: Writes[Study.Id] = stringIsoWriter(Study.idIso)
  implicit val studyNameWrites: Writes[Study.Name] = stringIsoWriter(Study.nameIso)
  implicit val chapterIdWrites: Writes[Chapter.Id] = stringIsoWriter(Chapter.idIso)
  implicit val chapterNameWrites: Writes[Chapter.Name] = stringIsoWriter(Chapter.nameIso)

  private implicit val uciWrites: Writes[Uci] = Writes[Uci] { u =>
    JsString(u.uci)
  }
  private implicit val uciCharPairWrites: Writes[UciCharPair] = Writes[UciCharPair] { u =>
    JsString(u.toString)
  }
  private implicit val posReader: Reads[Pos] = Reads[Pos] { v =>
    (v.asOpt[String] flatMap Pos.posAt).fold[JsResult[Pos]](JsError(Nil))(JsSuccess(_))
  }
  private[study] implicit val pathWrites: Writes[Path] = Writes[Path] { p =>
    JsString(p.toString)
  }
  private implicit val colorWriter: Writes[chess.Color] = Writes[chess.Color] { c =>
    JsString(c.name)
  }
  private implicit val fenWriter: Writes[FEN] = Writes[FEN] { f =>
    JsString(f.value)
  }
  private[study] implicit val uidWriter: Writes[Uid] = Writes[Uid] { uid =>
    JsString(uid.value)
  }
  private[study] implicit val visibilityWriter: Writes[Study.Visibility] = Writes[Study.Visibility] { v =>
    JsString(v.key)
  }
  private[study] implicit val fromWriter: Writes[Study.From] = Writes[Study.From] {
    case Study.From.Scratch => JsString("scratch")
    case Study.From.Game(id) => Json.obj("game" -> id)
    case Study.From.Study(id) => Json.obj("study" -> id)
  }
  private[study] implicit val userSelectionWriter = Writes[Settings.UserSelection] { v =>
    JsString(v.key)
  }
  private[study] implicit val settingsWriter: Writes[Settings] = Json.writes[Settings]

  private[study] implicit val shapeReader: Reads[Shape] = Reads[Shape] { js =>
    js.asOpt[JsObject].flatMap { o =>
      for {
        brush <- o str "brush"
        orig <- o.get[Pos]("orig")
      } yield o.get[Pos]("dest") match {
        case Some(dest) => Shape.Arrow(brush, orig, dest)
        case _ => Shape.Circle(brush, orig)
      }
    }.fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))
  }
  private implicit val plyWrites = Writes[Chapter.Ply] { p =>
    JsNumber(p.value)
  }

  private implicit val variantWrites = OWrites[chess.variant.Variant] { v =>
    Json.obj("key" -> v.key, "name" -> v.name)
  }
  implicit val pgnTagWrites: Writes[chess.format.pgn.Tag] = Writes[chess.format.pgn.Tag] { t =>
    Json.arr(t.name.toString, t.value)
  }
  private implicit val chapterSetupWrites = Json.writes[Chapter.Setup]
  private[study] implicit val chapterMetadataWrites = OWrites[Chapter.Metadata] { c =>
    Json.obj("id" -> c._id, "name" -> c.name)
  }

  private[study] implicit val positionRefWrites: Writes[Position.Ref] = Json.writes[Position.Ref]
  private implicit val likesWrites: Writes[Study.Likes] = Writes[Study.Likes] { p =>
    JsNumber(p.value)
  }
  private[study] implicit val likingRefWrites: Writes[Study.Liking] = Json.writes[Study.Liking]
}
