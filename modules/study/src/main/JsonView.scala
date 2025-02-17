package lila.study

import chess.Square
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.core.i18n.Translate
import lila.core.socket.Sri
import lila.tree.Node.Shape
import lila.core.pref.Pref

final class JsonView(
    studyRepo: StudyRepo,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor):

  import JsonView.given

  def full(
      study: Study,
      chapter: Chapter,
      previews: Option[ChapterPreview.AsJsons],
      fedNames: Option[JsObject],
      withMembers: Boolean
  )(using me: Option[Me], pref: Pref) =

    def allowed(selection: Settings => Settings.UserSelection): Boolean =
      Settings.UserSelection.allows(selection(study.settings), study, me.map(_.userId))

    for
      liked <- me.so(studyRepo.liked(study, _))
      relayPath = chapter.relay
        .filter(_.secondsSinceLastMove.exists(_ < 3600) || chapter.tags.outcome.isEmpty)
        .map(_.path)
        .filterNot(_.isEmpty)
      jsStudy =
        if withMembers || me.exists(study.canContribute) then study
        else study.copy(members = StudyMembers.empty)
    yield Json.toJsObject(jsStudy) ++ Json
      .obj(
        "liked" -> liked,
        "features" -> Json
          .obj(
            "cloneable" -> allowed(_.cloneable),
            "shareable" -> allowed(_.shareable),
            "chat"      -> allowed(_.chat)
          )
          .add("sticky", study.settings.sticky)
          .add("description", study.settings.description),
        "topics" -> study.topicsOrEmpty,
        "chapter" -> Json
          .obj(
            "id"      -> chapter.id,
            "ownerId" -> chapter.ownerId,
            "setup"   -> chapter.setup,
            "tags"    -> chapter.tagsExport,
            "features" -> Json.obj(
              "computer" -> allowed(_.computer),
              "explorer" -> allowed(_.explorer)
            )
          )
          .add("description", chapter.description)
          .add("serverEval", chapter.serverEval)
          .add("relayPath", relayPath)
          .pipe(addChapterMode(chapter))
      )
      .add("chapters", previews)
      .add("description", study.description)
      .add("federations", fedNames)
      .add("showRatings", pref.showRatings)

  def chapterConfig(c: Chapter) =
    Json
      .obj(
        "id"          -> c.id,
        "name"        -> c.name,
        "orientation" -> c.setup.orientation
      )
      .add("description", c.description)
      .pipe(addChapterMode(c))

  def pagerData(s: Study.WithChaptersAndLiked) =
    Json
      .obj(
        "id"        -> s.study.id,
        "name"      -> s.study.name,
        "liked"     -> s.liked,
        "likes"     -> s.study.likes,
        "updatedAt" -> s.study.updatedAt,
        "owner"     -> lightUserApi.sync(s.study.ownerId),
        "chapters"  -> s.chapters.take(Study.previewNbChapters),
        "topics"    -> s.study.topicsOrEmpty,
        "members"   -> s.study.members.members.values.take(Study.previewNbMembers)
      )
      .add("flair", s.study.flair)

  private def addChapterMode(c: Chapter)(js: JsObject): JsObject =
    js.add("practice", c.isPractice)
      .add("gamebook", c.isGamebook)
      .add("conceal", c.conceal)

  private[study] given Writes[StudyMember.Role] = Writes: r =>
    JsString(r.id)
  private[study] given Writes[StudyMember] = Writes: m =>
    Json.obj("user" -> lightUserApi.syncFallback(m.id), "role" -> m.role)

  private[study] given Writes[StudyMembers] = Writes: m =>
    Json.toJson(m.members)

  private given OWrites[Study] = OWrites: s =>
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
        "secondsSinceUpdate" -> (nowSeconds - s.updatedAt.toSeconds).toInt,
        "from"               -> s.from,
        "likes"              -> s.likes
      )
      .add("isNew" -> s.isNew)
      .add("flair" -> s.flair)

object JsonView:

  case class JsData(study: JsObject, analysis: JsObject)

  given OWrites[lila.core.study.IdName] = Json.writes

  def metadata(study: Study) = Json.obj(
    "id"        -> study.id,
    "name"      -> study.name,
    "createdAt" -> study.createdAt,
    "updatedAt" -> study.updatedAt
  )

  def glyphs(using Translate): JsObject =
    import lila.core.i18n.I18nKey.study as trans
    import chess.format.pgn.Glyph
    import Glyph.MoveAssessment.*
    import Glyph.PositionAssessment.*
    import Glyph.Observation.*
    Json.obj(
      "move" -> List(
        good.copy(name = trans.goodMove.txt()),
        mistake.copy(name = trans.mistake.txt()),
        brilliant.copy(name = trans.brilliantMove.txt()),
        blunder.copy(name = trans.blunder.txt()),
        interesting.copy(name = trans.interestingMove.txt()),
        dubious.copy(name = trans.dubiousMove.txt()),
        only.copy(name = trans.onlyMove.txt()),
        zugzwang.copy(name = trans.zugzwang.txt())
      ),
      "position" -> List(
        equal.copy(name = trans.equalPosition.txt()),
        unclear.copy(name = trans.unclearPosition.txt()),
        whiteSlightlyBetter.copy(name = trans.whiteIsSlightlyBetter.txt()),
        blackSlightlyBetter.copy(name = trans.blackIsSlightlyBetter.txt()),
        whiteQuiteBetter.copy(name = trans.whiteIsBetter.txt()),
        blackQuiteBetter.copy(name = trans.blackIsBetter.txt()),
        whiteMuchBetter.copy(name = trans.whiteIsWinning.txt()),
        blackMuchBetter.copy(name = trans.blackIsWinning.txt())
      ),
      "observation" -> List(
        novelty.copy(name = trans.novelty.txt()),
        development.copy(name = trans.development.txt()),
        initiative.copy(name = trans.initiative.txt()),
        attack.copy(name = trans.attack.txt()),
        counterplay.copy(name = trans.counterplay.txt()),
        timeTrouble.copy(name = trans.timeTrouble.txt()),
        compensation.copy(name = trans.withCompensation.txt()),
        withIdea.copy(name = trans.withTheIdea.txt())
      )
    )

  private given Reads[Square] = Reads: v =>
    (v.asOpt[String].flatMap { Square.fromKey(_) }).fold[JsResult[Square]](JsError(Nil))(JsSuccess(_))
  private[study] given Writes[Sri]                        = writeAs(_.value)
  private[study] given Writes[lila.core.study.Visibility] = writeAs(_.toString)
  private[study] given Writes[Study.From] = Writes:
    case Study.From.Scratch   => JsString("scratch")
    case Study.From.Game(id)  => Json.obj("game" -> id)
    case Study.From.Study(id) => Json.obj("study" -> id)
    case Study.From.Relay(id) => Json.obj("relay" -> id)
  private[study] given Writes[Settings.UserSelection] = Writes(v => JsString(v.key))
  private[study] given Writes[Settings]               = Json.writes

  private[study] given Reads[Shape] = Reads:
    _.asOpt[JsObject]
      .flatMap { o =>
        for
          brush <- o.str("brush")
          orig  <- o.get[Square]("orig")
        yield o.get[Square]("dest") match
          case Some(dest) => Shape.Arrow(brush, orig, dest)
          case _          => Shape.Circle(brush, orig)
      }
      .fold[JsResult[Shape]](JsError(Nil))(JsSuccess(_))

  given OWrites[chess.variant.Variant] = OWrites: v =>
    Json.obj("key" -> v.key, "name" -> v.name)
  given Writes[chess.format.pgn.Tag] = Writes: t =>
    Json.arr(t.name.toString, t.value)
  given Writes[chess.format.pgn.Tags] = Writes: tags =>
    JsArray(tags.value.map(Json.toJson))
  private given OWrites[Chapter.Setup] = Json.writes

  private[study] given Writes[Position.Ref] = Json.writes
  private[study] given Writes[Study.Liking] = Json.writes

  given OWrites[Chapter.Relay] = OWrites: r =>
    Json.obj(
      "path"      -> r.path,
      "thinkTime" -> r.secondsSinceLastMove
    )

  private[study] given Writes[Chapter.ServerEval] = Json.writes

  private[study] given OWrites[Who] = OWrites: w =>
    Json.obj("u" -> w.u, "s" -> w.sri)
