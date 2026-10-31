package lila.studySearch

import play.api.libs.json.*

import lila.core.study.StudyOrder

case class StudySearchQuery(
    study: Option[StudySearchQuery.StudyFilter],
    chapter: Option[StudySearchQuery.ChapterFilter],
    cursor: Option[Long],
    order: StudyOrder
)

object StudySearchQuery:
  case class StudyFilter(
      text: Option[String],
      name: Option[String],
      description: Option[String],
      topics: Option[String],
      owner: Option[String],
      member: Option[String]
  )

  case class ChapterFilter(
      text: Option[String],
      name: Option[String],
      description: Option[String],
      tags: Option[StudySearchApi.TagFilter]
  )

  private given Reads[chess.Color] = Reads:
    case JsString("white") => JsSuccess(chess.White)
    case JsString("black") => JsSuccess(chess.Black)
    case _ => JsError("Player color must be white or black")
  private given Reads[StudySearchApi.TagFilter] = Json.reads[StudySearchApi.TagFilter]
  private given Reads[StudyFilter] = Json.reads[StudyFilter]
  private given Reads[ChapterFilter] = Json.reads[ChapterFilter]

  private val studyFields = Set("text", "name", "description", "topics", "owner", "member")
  private val chapterFields = Set("text", "name", "description", "tags")
  private val tagFields =
    Set("variant", "eco", "opening", "player1", "player2", "fideId1", "fideId2", "color1", "color2", "event")

  def read(json: JsValue): JsResult[StudySearchQuery] =
    val query = (json \ "query").toOption.getOrElse(JsNull)
    val study = (query \ "study").toOption.filterNot(_ == JsNull)
    val chapter = (query \ "chapter").toOption.filterNot(_ == JsNull)
    val tags = (query \ "chapter" \ "tags").toOption.filterNot(_ == JsNull)
    if (json \ "version").asOpt[Int] != Some(1) ||
      (query \ "scope").toOption != Some(Json.obj("kind" -> "search")) ||
      !onlyFields(json, Set("version", "query", "cursor", "order")) ||
      !onlyFields(query, Set("scope", "study", "chapter")) ||
      !study.forall(onlyFields(_, studyFields)) ||
      !chapter.forall(onlyFields(_, chapterFields)) ||
      !tags.forall(onlyFields(_, tagFields))
    then JsError("Unsupported study search query")
    else
      for
        studyFilter <- (query \ "study").validateOpt[StudyFilter]
        chapterFilter <- (query \ "chapter").validateOpt[ChapterFilter]
        cursor <- (json \ "cursor").validateOpt[Long]
        order <- (json \ "order")
          .validateOpt[String]
          .flatMap:
            case None => JsSuccess(StudyOrder.relevant)
            case Some(key) =>
              StudyOrder.byKey.get(key).filterNot(_ == StudyOrder.mine) match
                case Some(order) => JsSuccess(order)
                case None => JsError("Unsupported study search order")
        result <-
          val studyValues = study.toList.flatMap(strings)
          val chapterValues = chapter.toList.flatMap(strings)
          val values = studyValues ++ chapterValues
          if cursor.exists(value => value < 0 || value >= 10000) then JsError("Cursor must be 0-9999")
          else if values.exists(value => value.trim.isEmpty || value.length > 200) then
            JsError("Search criteria must be 1-200 characters per field")
          else
            JsSuccess(
              StudySearchQuery(
                studyFilter.filter(_ => studyValues.nonEmpty),
                chapterFilter.filter(_ => chapterValues.nonEmpty),
                cursor,
                order
              )
            )
      yield result

  private def onlyFields(json: JsValue, allowed: Set[String]): Boolean = json match
    case obj: JsObject => obj.keys.subsetOf(allowed)
    case _ => false

  private def strings(json: JsValue): List[String] = json match
    case JsString(value) => List(value)
    case obj: JsObject => obj.values.toList.flatMap(strings)
    case _ => Nil
