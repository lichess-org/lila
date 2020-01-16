package lila.clas

import org.joda.time.DateTime

case class Clas(
    _id: Clas.Id,
    name: String,
    desc: String,
    ownerId: Teacher.Id,
    createdAt: DateTime,
    updatedAt: DateTime
) {}

object Clas {

  case class WithOwner(clas: Clas, teacher: Teacher)

  case class Id(value: String) extends AnyVal with StringValue
}
