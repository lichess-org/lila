package lila.study

import reactivemongo.api.ReadPreference

import lila.db.dsl._

final class ChapterRepo(coll: Coll) {

  import BSONHandlers._

  val maxChapters = 64

  val noRootProjection = $doc("root" -> false)

  def byId(id: Chapter.Id): Fu[Option[Chapter]] = coll.byId[Chapter, Chapter.Id](id)

  // def metadataById(id: Chapter.Id): Fu[Option[Chapter.Metadata]] =
  // coll.find($id(id), noRootProjection).one[Chapter.Metadata]

  def deleteByStudy(s: Study): Funit = coll.remove($studyId(s.id)).void

  def byIdAndStudy(id: Chapter.Id, studyId: Study.Id): Fu[Option[Chapter]] =
    coll.byId[Chapter, Chapter.Id](id).map { _.filter(_.studyId == studyId) }

  def firstByStudy(studyId: Study.Id): Fu[Option[Chapter]] =
    coll.find($studyId(studyId)).sort($sort asc "order").one[Chapter]

  def orderedMetadataByStudy(studyId: Study.Id): Fu[List[Chapter.Metadata]] =
    coll.find(
      $studyId(studyId),
      noRootProjection
    ).sort($sort asc "order").list[Chapter.Metadata](maxChapters)

  // loads all study chapters in memory! only used for search indexing and cloning
  def orderedByStudy(studyId: Study.Id): Fu[List[Chapter]] =
    coll.find($studyId(studyId))
      .sort($sort asc "order")
      .cursor[Chapter](readPreference = ReadPreference.secondaryPreferred)
      .gather[List](maxChapters)

  def sort(study: Study, ids: List[Chapter.Id]): Funit = ids.zipWithIndex.map {
    case (id, index) =>
      coll.updateField($studyId(study.id) ++ $id(id), "order", index + 1)
  }.sequenceFu.void

  def nextOrderByStudy(studyId: Study.Id): Fu[Int] =
    coll.primitiveOne[Int](
      $studyId(studyId),
      $sort desc "order",
      "order"
    ) map { order => ~order + 1 }

  def setConceal(chapterId: Chapter.Id, conceal: Chapter.Ply) =
    coll.updateField($id(chapterId), "conceal", conceal).void

  def removeConceal(chapterId: Chapter.Id) =
    coll.unsetField($id(chapterId), "conceal").void

  def setTagsFor(chapter: Chapter) =
    coll.updateField($id(chapter.id), "tags", chapter.tags).void

  private[study] def namesByStudyIds(studyIds: Seq[Study.Id]): Fu[Map[Study.Id, Vector[String]]] =
    coll.find(
      $doc("studyId" $in studyIds),
      $doc("studyId" -> true, "name" -> true)
    ).sort($sort asc "order").list[Bdoc]().map { docs =>
        docs.foldLeft(Map.empty[Study.Id, Vector[String]]) {
          case (hash, doc) => {
            for {
              studyId <- doc.getAs[Study.Id]("studyId")
              name <- doc.getAs[String]("name")
            } yield hash + (studyId -> (hash.get(studyId) match {
              case None        => Vector(name)
              case Some(names) => names :+ name
            }))
          } | hash
        }
      }

  def countByStudyId(studyId: Study.Id): Fu[Int] =
    coll.countSel($studyId(studyId))

  def insert(s: Chapter): Funit = coll.insert(s).void

  def update(c: Chapter): Funit = coll.update($id(c.id), c).void

  def delete(id: Chapter.Id): Funit = coll.remove($id(id)).void
  def delete(c: Chapter): Funit = delete(c.id)

  private def $studyId(id: Study.Id) = $doc("studyId" -> id)
}
