package lila.study

import chess.format.{ Fen, Uci }
import chess.Pos
import play.api.libs.json.*
import scala.util.chaining.*

import lila.common.Json.{ *, given }
import lila.socket.Socket.Sri
import lila.tree.Node.Shape
import lila.user.User

final class JsonView(
    studyRepo: StudyRepo,
    lightUserApi: lila.user.LightUserApi
)(using ec: scala.concurrent.ExecutionContext):

  import JsonView.{ *, given }

  def apply(
      study: Study,
      chapters: List[Chapter.Metadata],
      currentChapter: Chapter,
      me: Option[User]
  ) =

    def allowed(selection: Settings => Settings.UserSelection): Boolean =
      Settings.UserSelection.allows(selection(study.settings), study, me.map(_.id))

    me.?? { studyRepo.liked(study, _) } map { liked =>
      Json.toJsObject(study) ++ Json
        .obj(
          "liked" -> liked,
          "features" -> Json.obj(
            "cloneable"   -> allowed(_.cloneable),
            "shareable"   -> allowed(_.shareable),
            "chat"        -> allowed(_.chat),
            "sticky"      -> study.settings.sticky,
            "description" -> study.settings.description
          ),
          "topics"   -> study.topicsOrEmpty,
          "chapters" -> chapters,
          "chapter" -> Json
            .obj(
              "id"      -> currentChapter.id,
              "ownerId" -> currentChapter.ownerId,
              "setup"   -> currentChapter.setup,
              "tags"    -> currentChapter.tags,
              "features" -> Json.obj(
                "computer" -> allowed(_.computer),
                "explorer" -> allowed(_.explorer)
              )
            )
            .add("description", currentChapter.description)
            .add("serverEval", currentChapter.serverEval)
            .add("relay", currentChapter.relay)
            .pipe(addChapterMode(currentChapter))
        )
        .add("description", study.description)
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
      "id"        -> s.study.id,
      "name"      -> s.study.name,
      "liked"     -> s.liked,
      "likes"     -> s.study.likes,
      "updatedAt" -> s.study.updatedAt,
      "owner"     -> lightUserApi.sync(s.study.ownerId),
      "chapters"  -> s.chapters.take(Study.previewNbChapters),
      "members"   -> s.study.members.members.values.take(Study.previewNbMembers)
    )

  private def addChapterMode(c: Chapter)(js: JsObject): JsObject =
    js.add("practice", c.isPractice)
      .add("gamebook", c.isGamebook)
      .add("conceal", c.conceal)

  private[study] given Writes[StudyMember.Role] = Writes { r =>
    JsString(r.id)
  }
  private[study] given Writes[StudyMember] = Writes { m =>
    Json.obj("user" -> lightUserApi.syncFallback(m.id), "role" -> m.role)
  }

  private[study] given Writes[StudyMembers] = Writes { m =>
    Json toJson m.members
  }

  private given OWrites[Study] = OWrites { s =>
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

object JsonView:

  case class JsData(study: JsObject, analysis: JsObject)

  import Study.given

  given OWrites[Study.IdName] = OWrites { s =>
    Json.obj("id" -> s._id, "name" -> s.name)
  }

  private given Reads[Pos] = Reads { v =>
    (v.asOpt[String] flatMap { Pos.fromKey(_) }).fold[JsResult[Pos]](JsError(Nil))(JsSuccess(_))
  }
  private[study] given Writes[Path]             = Writes(p => JsString(p.toString))
  private[study] given Writes[Sri]              = Writes(s => JsString(s.value))
  private[study] given Writes[Study.Visibility] = Writes(v => JsString(v.key))
  private[study] given Writes[Study.From] = Writes {
    case Study.From.Scratch   => JsString("scratch")
    case Study.From.Game(id)  => Json.obj("game" -> id)
    case Study.From.Study(id) => Json.obj("study" -> id)
    case Study.From.Relay(id) => Json.obj("relay" -> id)
  }
  private[study] given Writes[Settings.UserSelection] = Writes(v => JsString(v.key))
  private[study] given Writes[Settings]               = Json.writes

  private[study] given Reads[Shape] = Reads { js =>
    js.asOpt[JsObject]
      .flatMap { o =>
        for {
          brush <- o str "brush"
          orig  <- o.get[Pos]("orig")
        } yield o.get[Pos]("dest") match
          case Some(dest) => Shape.Arrow(brush, orig, dest)
          case _          => Shape.Circle(brush, orig)
      }
      .fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))
  }
  private given Writes[Chapter.Ply] = Writes(p => JsNumber(p.value))

  given OWrites[chess.variant.Variant] = OWrites { v =>
    Json.obj("key" -> v.key, "name" -> v.name)
  }
  given Writes[chess.format.pgn.Tag] = Writes { t =>
    Json.arr(t.name.toString, t.value)
  }
  given Writes[chess.format.pgn.Tags] = Writes { tags =>
    JsArray(tags.value map Json.toJson)
  }
  private given OWrites[Chapter.Setup] = Json.writes

  given OWrites[Chapter.Metadata] = OWrites { c =>
    Json
      .obj("id" -> c._id, "name" -> c.name)
      .add("ongoing", c.looksOngoing)
      .add("res" -> c.resultStr)
  }

  private[study] given Writes[Position.Ref] = Json.writes
  private[study] given Writes[Study.Liking] = Json.writes

  given OWrites[Chapter.Relay] = OWrites { r =>
    Json.obj(
      "path"                 -> r.path,
      "secondsSinceLastMove" -> r.secondsSinceLastMove
    )
  }

  private[study] given Writes[Chapter.ServerEval] = Json.writes

  private[study] given OWrites[actorApi.Who] = OWrites { w =>
    Json.obj("u" -> w.u, "s" -> w.sri)
  }

  import StudyTopic.given
  given Writes[StudyTopics] = Writes { topics =>
    JsArray(topics.value map Json.toJson)
  }
