package lila.clas

import lila.user.User

import org.joda.time.DateTime

case class Student(
    _id: Student.Id, // userId:clasId
    userId: User.ID,
    clasId: Clas.Id,
    realName: String,
    notes: String,
    managed: Boolean, // created for the class by the teacher
    created: Clas.Recorded,
    archived: Option[Clas.Recorded]
) {

  def id = _id

  def is(user: User)     = userId == user.id
  def is(other: Student) = id == other.id

  def isArchived = archived.isDefined
  def isActive   = !isArchived
}

object Student {

  def id(userId: User.ID, clasId: Clas.Id) = Id(s"$userId:$clasId")

  def make(user: User, clas: Clas, teacherId: User.ID, realName: String, managed: Boolean) =
    Student(
      _id = id(user.id, clas.id),
      userId = user.id,
      clasId = clas.id,
      realName = realName,
      notes = "",
      managed = managed,
      created = Clas.Recorded(teacherId, DateTime.now),
      archived = none
    )

  case class Id(value: String) extends AnyVal with StringValue

  case class WithUser(student: Student, user: User)

  case class WithPassword(student: Student, password: User.ClearPassword)

  case class ManagedInfo(createdBy: User, clas: Clas)

  private[clas] object password {

    private val random     = new java.security.SecureRandom()
    private val chars      = ('2' to '9') ++ (('a' to 'z').toSet - 'l') mkString
    private val nbChars    = chars.length
    private def secureChar = chars(random nextInt nbChars)

    def generate =
      User.ClearPassword {
        new String(Array.fill(7)(secureChar))
      }
  }
}
