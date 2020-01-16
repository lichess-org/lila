package lila.clas

import lila.user.User

import org.joda.time.DateTime

case class Student(
    _id: Student.Id, // userId:clasId
    userId: User.ID,
    clasId: Clas.Id,
    managed: Boolean, // created for the class by the teacher
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def is(user: User) = userId == user.id
}

object Student {

  def id(userId: User.ID, clasId: Clas.Id) = Id(s"${userId}:${clasId}")

  def make(user: User, clas: Clas, managed: Boolean) = Student(
    _id = id(user.id, clas.id),
    userId = user.id,
    clasId = clas.id,
    managed = managed,
    createdAt = DateTime.now,
    updatedAt = DateTime.now
  )

  case class Id(value: String) extends AnyVal with StringValue

  case class WithUser(student: Student, user: User)
}
