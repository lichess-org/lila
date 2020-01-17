package lila.clas

import scalaz.NonEmptyList
import org.joda.time.DateTime

case class Clas(
    _id: Clas.Id,
    name: String,
    desc: String,
    teachers: NonEmptyList[Teacher.Id], // first is owner
    nbStudents: Int,
    created: Clas.Recorded,
    viewedAt: DateTime,
    archived: Option[Clas.Recorded]
) {

  def id = _id
}

object Clas {

  def make(teacher: Teacher, name: String, desc: String) = Clas(
    _id = Id(scala.util.Random.alphanumeric take 8 mkString),
    name = name,
    desc = desc,
    teachers = NonEmptyList(teacher.id),
    nbStudents = 0,
    created = Recorded(teacher.id, DateTime.now),
    viewedAt = DateTime.now,
    archived = none
  )

  case class WithOwner(clas: Clas, teacher: Teacher)

  case class Id(value: String) extends AnyVal with StringValue

  case class Recorded(by: Teacher.Id, at: DateTime)

  // case class WithAll(
  //     clas: Clas,
  //     teachers: List[Teacher],
  //     students: List[Student]
  // ) {
  //   def userIds = teachers.map(_.userId) ::: students.map(_.userId)
  // }
}
