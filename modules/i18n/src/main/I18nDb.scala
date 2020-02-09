package lila.i18n

import play.api.i18n.Lang

object I18nDb {

  sealed trait Ref
  case object Site        extends Ref
  case object Arena       extends Ref
  case object Emails      extends Ref
  case object Learn       extends Ref
  case object Activity    extends Ref
  case object Coordinates extends Ref
  case object Study       extends Ref
  case object Clas        extends Ref
  case object Contact     extends Ref
  case object Patron      extends Ref
  case object Coach       extends Ref
  case object Broadcast   extends Ref

  val site: Messages        = lila.i18n.db.site.Registry.load
  val arena: Messages       = lila.i18n.db.arena.Registry.load
  val emails: Messages      = lila.i18n.db.emails.Registry.load
  val learn: Messages       = lila.i18n.db.learn.Registry.load
  val activity: Messages    = lila.i18n.db.activity.Registry.load
  val coordinates: Messages = lila.i18n.db.coordinates.Registry.load
  val study: Messages       = lila.i18n.db.study.Registry.load
  val clas: Messages        = lila.i18n.db.clas.Registry.load
  val contact: Messages     = lila.i18n.db.contact.Registry.load
  val patron: Messages      = lila.i18n.db.patron.Registry.load
  val coach: Messages       = lila.i18n.db.coach.Registry.load
  val broadcast: Messages   = lila.i18n.db.broadcast.Registry.load

  def apply(ref: Ref): Messages = ref match {
    case Site        => site
    case Arena       => arena
    case Emails      => emails
    case Learn       => learn
    case Activity    => activity
    case Coordinates => coordinates
    case Study       => study
    case Clas        => clas
    case Contact     => contact
    case Patron      => patron
    case Coach       => coach
    case Broadcast   => broadcast
  }

  val langs: Set[Lang] = site.keys.toSet
}
