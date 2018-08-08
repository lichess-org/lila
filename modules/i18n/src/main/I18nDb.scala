package lidraughts.i18n

object I18nDb {

  sealed trait Ref
  case object Site extends Ref
  case object Arena extends Ref
  case object Emails extends Ref
  case object Learn extends Ref
  case object Activity extends Ref
  case object Coordinates extends Ref

  val site: Messages = lidraughts.i18n.db.site.Registry.load
  val arena: Messages = lidraughts.i18n.db.arena.Registry.load
  val emails: Messages = lidraughts.i18n.db.emails.Registry.load
  val learn: Messages = lidraughts.i18n.db.learn.Registry.load
  val activity: Messages = lidraughts.i18n.db.activity.Registry.load
  val coordinates: Messages = lidraughts.i18n.db.coordinates.Registry.load

  def apply(ref: Ref): Messages = ref match {
    case Site => site
    case Arena => arena
    case Emails => emails
    case Learn => learn
    case Activity => activity
    case Coordinates => coordinates
  }

  val langs = site.keySet
}
