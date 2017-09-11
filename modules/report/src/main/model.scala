package lila.report

import lila.user.User

case class Mod(user: User) extends AnyVal

case class Suspect(user: User) extends AnyVal {

  def set(f: User => User) = copy(user = f(user))
}

case class Victim(user: User) extends AnyVal
