package lila.study

import chess.format.{ FEN, Uci }
import chess.Pos
import play.api.libs.json._
import scala.util.chaining._

import lila.common.Json._
import lila.socket.Socket.Sri
import lila.tree.Node.Shape
import lila.user.User

final class JsonView(
    studyRepo: StudyRepo,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

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
      studyWrites.writes(study) ++ Json
        .obj(
          "liked" -> liked,
          "features" -> Json.obj(
            "cloneable"   -> allowed(study.settings.cloneable),
            "chat"        -> allowed(study.settings.chat),
            "sticky"      -> study.settings.sticky,
            "description" -> study.settings.description
          ),
          "topics"   -> study.topicsOrEmpty,
          "chapters" -> chapters.map(chapterMetadataWrites.writes),
          "chapter" -> Json
            .obj(
              "id"      -> currentChapter.id,
              "ownerId" -> currentChapter.ownerId,
              "setup"   -> currentChapter.setup,
              "tags"    -> currentChapter.tags,
              "features" -> Json.obj(
                "computer" -> allowed(study.settings.computer),
                "explorer" -> allowed(study.settings.explorer)
              )
            )
            .add("description", currentChapter.description)
            .add("serverEval", currentChapter.serverEval)
            .add("relay", currentChapter.relay)(relayWrites)
            .pipe(addChapterMode(currentChapter))
        )
        .add("description", study.description)
    }
  }

  def chapterConfig(c: Chapter) =
    Json
      .obj(
        "id"          -> c.id,
        "name"        -> c.name,
        "orientation" -> c.setup.orientation
      )
      .add("description", c.description) pipe addChapterMode(c)

  def pagerData(s: Study.WithChaptersAndLiked) =
    Json.obj(
      "id"        -> s.study.id.value,
      "name"      -> s.study.name.value,
      "liked"     -> s.liked,
      "likes"     -> s.study.likes.value,
      "updatedAt" -> s.study.updatedAt,
      "owner"     -> lightUserApi.sync(s.study.ownerId),
      "chapters"  -> s.chapters.take(4),
      "members"   -> s.study.members.members.values.take(4)
    )

  private def addChapterMode(c: Chapter)(js: JsObject): JsObject =
    js.add("practice", c.isPractice)
      .add("gamebook", c.isGamebook)
      .add("conceal", c.conceal)

  implicit private[study] val memberRoleWrites = Writes[StudyMember.Role] { r =>
    JsString(r.id)
  }
  implicit private[study] val memberWrites: Writes[StudyMember] = Writes[StudyMember] { m =>
    Json.obj("user" -> lightUserApi.syncFallback(m.id), "role" -> m.role)
  }

  implicit private[study] val membersWrites: Writes[StudyMembers] = Writes[StudyMembers] { m =>
    Json toJson m.members
  }

  implicit private val studyWrites = OWrites[Study] { s =>
    Json
      .obj(
        "id"                 -> s.id,
        "name"               -> s.name,
        "members"            -> s.members,
        "position"           -> s.position,
        "ownerId"            -> s.ownerId,
        "settings"           -> s.settings,
        "visibility"         -> s.visibility,
        "createdAt"          -> s.createdAt,
        "secondsSinceUpdate" -> (nowSeconds - s.updatedAt.getSeconds).toInt,
        "from"               -> s.from,
        "likes"              -> s.likes
      )
      .add("isNew" -> s.isNew)
  }
}

object JsonView {

  case class JsData(study: JsObject, analysis: JsObject)

  implicit val studyIdWrites: Writes[Study.Id]     = stringIsoWriter(Study.idIso)
  implicit val studyNameWrites: Writes[Study.Name] = stringIsoWriter(Study.nameIso)
  implicit val studyIdNameWrites = OWrites[Study.IdName] { s =>
    Json.obj("id" -> s._id, "name" -> s.name)
  }
  implicit val chapterIdWrites: Writes[Chapter.Id]     = stringIsoWriter(Chapter.idIso)
  implicit val chapterNameWrites: Writes[Chapter.Name] = stringIsoWriter(Chapter.nameIso)

  implicit private[study] val uciWrites: Writes[Uci] = Writes[Uci] { u =>
    JsString(u.uci)
  }
  implicit private val posReader: Reads[Pos] = Reads[Pos] { v =>
    (v.asOpt[String] flatMap Pos.fromKey).fold[JsResult[Pos]](JsError(Nil))(JsSuccess(_))
  }
  implicit private[study] val pathWrites: Writes[Path] = Writes[Path] { p =>
    JsString(p.toString)
  }
  implicit private[study] val colorWriter: Writes[chess.Color] = Writes[chess.Color] { c =>
    JsString(c.name)
  }
  implicit private[study] val fenWriter: Writes[FEN] = Writes[FEN] { f =>
    JsString(f.value)
  }
  implicit private[study] val sriWriter: Writes[Sri] = Writes[Sri] { sri =>
    JsString(sri.value)
  }
  implicit private[study] val visibilityWriter: Writes[Study.Visibility] = Writes[Study.Visibility] { v =>
    JsString(v.key)
  }
  implicit private[study] val fromWriter: Writes[Study.From] = Writes[Study.From] {
    case Study.From.Scratch   => JsString("scratch")
    case Study.From.Game(id)  => Json.obj("game" -> id)
    case Study.From.Study(id) => Json.obj("study" -> id)
    case Study.From.Relay(id) => Json.obj("relay" -> id)
  }
  implicit private[study] val userSelectionWriter = Writes[Settings.UserSelection] { v =>
    JsString(v.key)
  }
  implicit private[study] val settingsWriter: Writes[Settings] = Json.writes[Settings]

  implicit private[study] val shapeReader: Reads[Shape] = Reads[Shape] { js =>
    js.asOpt[JsObject]
      .flatMap { o =>
        for {
          brush <- o str "brush"
          orig  <- o.get[Pos]("orig")
        } yield o.get[Pos]("dest") match {
          case Some(dest) => Shape.Arrow(brush, orig, dest)
          case _          => Shape.Circle(brush, orig)
        }
      }
      .fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))
  }
  implicit private val plyWrites = Writes[Chapter.Ply] { p =>
    JsNumber(p.value)
  }

  implicit private val variantWrites = OWrites[chess.variant.Variant] { v =>
    Json.obj("key" -> v.key, "name" -> v.name)
  }
  implicit val pgnTagWrites: Writes[chess.format.pgn.Tag] = Writes[chess.format.pgn.Tag] { t =>
    Json.arr(t.name.toString, t.value)
  }
  implicit val pgnTagsWrites = Writes[chess.format.pgn.Tags] { tags =>
    JsArray(tags.value map pgnTagWrites.writes)
  }
  implicit private val chapterSetupWrites = Json.writes[Chapter.Setup]
  implicit private[study] val chapterMetadataWrites = OWrites[Chapter.Metadata] { c =>
    Json
      .obj("id" -> c._id, "name" -> c.name)
      .add("ongoing", c.looksOngoing)
      .add("res" -> c.resultStr)
  }

  implicit private[study] val positionRefWrites: Writes[Position.Ref] = Json.writes[Position.Ref]
  implicit private val likesWrites: Writes[Study.Likes] = Writes[Study.Likes] { p =>
    JsNumber(p.value)
  }
  implicit private[study] val likingRefWrites: Writes[Study.Liking] = Json.writes[Study.Liking]

  implicit val relayWrites = OWrites[Chapter.Relay] { r =>
    Json.obj(
      "path"                 -> r.path,
      "secondsSinceLastMove" -> r.secondsSinceLastMove
    )
  }

  implicit private[study] val serverEvalWrites: Writes[Chapter.ServerEval] = Json.writes[Chapter.ServerEval]

  implicit private[study] val whoWriter: Writes[actorApi.Who] = Writes[actorApi.Who] { w =>
    Json.obj("u" -> w.u, "s" -> w.sri)
  }

  implicit val topicWrites: Writes[StudyTopic] = stringIsoWriter(StudyTopic.topicIso)
  implicit val topicsWrites: Writes[StudyTopics] = Writes[StudyTopics] { topics =>
    JsArray(topics.value map topicWrites.writes)
  }
}
