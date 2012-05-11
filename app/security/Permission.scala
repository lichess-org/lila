package lila
package security

sealed abstract class Permission(val name: String)

case object Anonymous extends Permission("ANON")
case object Admin extends Permission("ROLE_ADMIN")
case object SuperAdmin extends Permission("ROLE_SUPER_ADMIN")

object Permission {

  val all: List[Permission] = List(SuperAdmin, Admin)
  val allByName: Map[String, Permission] = all map { p =>
    (p.name, p)
  } toMap

  def apply(name: String): Option[Permission] = allByName get name

  def apply(names: List[String]): List[Permission] = (names map apply).flatten
}
