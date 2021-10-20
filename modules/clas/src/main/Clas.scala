package lila.clas

import cats.data.NonEmptyList
import org.joda.time.DateTime

import lila.user.User

case class Clas(
    _id: Clas.Id,
    name: String,
    desc: String,
    wall: String = "",
    teachers: NonEmptyList[User.ID], // first is owner
    created: Clas.Recorded,
    viewedAt: DateTime,
    archived: Option[Clas.Recorded]
) {

  def id = _id

  def withStudents(students: List[Student]) = Clas.WithStudents(this, students)

  def isArchived = archived.isDefined
  def isActive   = !isArchived
}

object Clas {

  val maxStudents = 100

  def make(teacher: User, name: String, desc: String) =
    Clas(
      _id = Id(lila.common.ThreadLocalRandom nextString 8),
      name = name,
      desc = desc,
      teachers = NonEmptyList.one(teacher.id),
      created = Recorded(teacher.id, DateTime.now),
      viewedAt = DateTime.now,
      archived = none
    )

  case class WithOwner(clas: Clas, teacher: Teacher)

  case class Id(value: String) extends AnyVal with StringValue

  case class Recorded(by: User.ID, at: DateTime)

  case class WithStudents(clas: Clas, students: List[Student])
}
