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
  case object Streamer    extends Ref
  case object Tfa         extends Ref
  case object Settings    extends Ref
  case object Preferences extends Ref
  case object Team        extends Ref
  case object PerfStat    extends Ref
  case object Search      extends Ref

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
  val streamer: Messages    = lila.i18n.db.streamer.Registry.load
  val tfa: Messages         = lila.i18n.db.tfa.Registry.load
  val settings: Messages    = lila.i18n.db.settings.Registry.load
  val preferences: Messages = lila.i18n.db.preferences.Registry.load
  val team: Messages        = lila.i18n.db.team.Registry.load
  val perfStat: Messages    = lila.i18n.db.perfStat.Registry.load
  val search: Messages      = lila.i18n.db.search.Registry.load

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
    case Streamer    => streamer
    case Tfa         => tfa
    case Settings    => settings
    case Preferences => preferences
    case Team        => team
    case PerfStat    => perfStat
    case Search      => search
  }

  val langs: Set[Lang] = site.keys.toSet
}
