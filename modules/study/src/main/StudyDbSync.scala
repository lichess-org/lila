package lila.study

import org.apache.pekko.stream.scaladsl.Source
import play.api.libs.json.*

enum StudyDbSync:
  case Sync(manifest: Map[StudyId, Long])
  case Fetch(ids: List[StudyId])

  def changes(source: Source[Study, ?]): Source[StudyDbSync.Change, ?] =
    import StudyDbSync.Change
    this match
      case Sync(manifest) =>
        source
          .map[Option[Study]](Some(_))
          .concat(Source.single(None)) // none is sentinel
          .statefulMapConcat: () =>
            val missing = scala.collection.mutable.Set.from(manifest.keySet)
            (study: Option[Study]) =>
              study match
                case Some(value) =>
                  missing -= value.id
                  if manifest.get(value.id).contains(value.metadataRevision) then Nil
                  else List(Change.Upsert(value))
                case None => missing.toList.map(Change.Delete.apply) :+ Change.Complete
      case Fetch(_) => source.map(Change.Upsert.apply).concat(Source.single(Change.Complete))

object StudyDbSync:
  enum Change:
    case Upsert(study: Study)
    case Delete(id: StudyId)
    case Complete

  def read(json: JsValue): JsResult[StudyDbSync] =
    (json \ "mode")
      .validate[String]
      .flatMap:
        case "sync" =>
          (json \ "manifest")
            .validate[Map[String, Long]]
            .flatMap: manifest =>
              if manifest.size > 100000 || manifest.values.exists(_ < 0)
              then JsError("bad study sync manifest")
              else JsSuccess(Sync(manifest.map((id, revision) => StudyId(id) -> revision)))
        case "fetch" =>
          (json \ "ids")
            .validate[List[String]]
            .flatMap: ids =>
              if ids.size > 100 then JsError("bad study fetch ids")
              else JsSuccess(Fetch(ids.distinct.map(StudyId.apply)))
        case _ => JsError("bad study db op")
