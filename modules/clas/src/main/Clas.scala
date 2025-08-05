package lila.clas

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.id.ClasId

case class Clas(
    @Key("_id") id: ClasId,
    name: String,
    desc: String,
    wall: Markdown = Markdown(""),
    teachers: NonEmptyList[UserId], // first is owner
    created: Clas.Recorded,
    viewedAt: Instant,
    archived: Option[Clas.Recorded]
):
  def withStudents(students: List[Student]) = Clas.WithStudents(this, students)

  def isArchived = archived.isDefined
  def isActive = !isArchived

object Clas:

  val maxStudents = 100

  def make(teacher: User, name: String, desc: String) =
    Clas(
      id = ClasId(scalalib.ThreadLocalRandom.nextString(8)),
      name = name,
      desc = desc,
      teachers = NonEmptyList.one(teacher.id),
      created = Recorded(teacher.id, nowInstant),
      viewedAt = nowInstant,
      archived = none
    )

  case class Recorded(by: UserId, at: Instant)

  case class WithStudents(clas: Clas, students: List[Student])
