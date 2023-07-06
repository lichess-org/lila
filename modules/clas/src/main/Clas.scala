package lila.clas

import ornicar.scalalib.ThreadLocalRandom

import lila.user.User

case class Clas(
    _id: Clas.Id,
    name: String,
    desc: String,
    wall: Markdown = Markdown(""),
    teachers: NonEmptyList[UserId], // first is owner
    created: Clas.Recorded,
    viewedAt: Instant,
    archived: Option[Clas.Recorded]
):

  inline def id = _id

  def withStudents(students: List[Student]) = Clas.WithStudents(this, students)

  def isArchived = archived.isDefined
  def isActive   = !isArchived

object Clas:

  val maxStudents = 100

  def make(teacher: User, name: String, desc: String) =
    Clas(
      _id = Id(ThreadLocalRandom nextString 8),
      name = name,
      desc = desc,
      teachers = NonEmptyList.one(teacher.id),
      created = Recorded(teacher.id, nowInstant),
      viewedAt = nowInstant,
      archived = none
    )

  opaque type Id = String
  object Id extends OpaqueString[Id]

  case class Recorded(by: UserId, at: Instant)

  case class WithStudents(clas: Clas, students: List[Student])
