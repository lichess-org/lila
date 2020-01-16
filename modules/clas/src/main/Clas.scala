package lila.clas

import scalaz.NonEmptyList
import org.joda.time.DateTime

case class Clas(
    _id: Clas.Id,
    name: String,
    desc: String,
    teachers: NonEmptyList[Teacher.Id], // first is owner
    nbStudents: Int,
    createdAt: DateTime,
    updatedAt: DateTime,
    viewedAt: DateTime
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
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    viewedAt = DateTime.now
  )

  case class WithOwner(clas: Clas, teacher: Teacher)

  case class Id(value: String) extends AnyVal with StringValue
}
