package lila.clas

import lila.user.User

import org.joda.time.DateTime

case class Teacher(
    _id: Teacher.Id, // user ID
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def userId = _id.value

  def is(user: User) = id.value == user.id
}

object Teacher {

  def make(user: User) =
    Teacher(
      _id = Id(user.id),
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

  case class Id(value: String) extends AnyVal with StringValue

  case class WithUser(teacher: Teacher, user: User)
}
