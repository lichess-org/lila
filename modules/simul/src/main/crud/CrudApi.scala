package lidraughts.simul
package crud

import org.joda.time.DateTime
import play.api.data.Form

import BSONHandlers._
import lidraughts.common.paginator.Paginator
import lidraughts.db.dsl._
import lidraughts.db.paginator.Adapter
import lidraughts.user.{ User, UserRepo }

final class CrudApi(simulRepo: SimulRepo) {

  def list = simulRepo uniques 50

  def one(id: String) = simulRepo uniqueById id

  def editForm(simul: Simul): Fu[Form[CrudForm.Data]] = UserRepo.byId(simul.hostId) flatMap { host =>
    UserRepo.byId(simul.arbiterId) map { arbiter =>
      editForm(simul, host, arbiter)
    }
  }

  def editForm(simul: Simul, host: Option[User], arbiter: Option[User]): Form[CrudForm.Data] = CrudForm.apply fill CrudForm.Data(
    name = simul.name,
    homepageHours = ~simul.spotlight.flatMap(_.homepageHours),
    date = simul.spotlight.fold(DateTime.now plusDays 7)(_.startsAt),
    image = ~simul.spotlight.flatMap(_.iconImg),
    headline = simul.spotlight.??(_.headline),
    description = simul.spotlight.??(_.description),
    hostName = host.??(_.username),
    arbiterName = arbiter.??(_.username),
    arbiterHidden = ~simul.spotlight.flatMap(_.arbiterHidden),
    clockTime = simul.clock.config.limitSeconds / 60,
    clockIncrement = simul.clock.config.incrementSeconds,
    clockExtra = simul.clock.hostExtraTime / 60,
    variants = simul.variants.map(_.id),
    color = simul.color.getOrElse(DataForm.colorDefault),
    chat = simul.spotlight.flatMap(_.chatmode).fold(DataForm.chatDefault)(_.key),
    ceval = simul.spotlight.flatMap(_.ceval).fold(CrudForm.cevalDefault)(_.key),
    percentage = ~simul.targetPct.map(_.toString),
    fmjd = simul.spotlight.flatMap(_.fmjdRating).fold(CrudForm.fmjdDefault)(_.key),
    drawLimit = ~simul.spotlight.flatMap(_.drawLimit).map(_.toString),
    noAssistance = ~simul.spotlight.flatMap(_.noAssistance)
  )

  def update(old: Simul, data: CrudForm.Data, host: User, arbiter: Option[User]) = {
    val upd = updateSimul(old, data, host, arbiter)
    simulRepo.update(upd) >>- {
      Env.current.api.socketStanding(upd, none)
    }
  }

  def createForm = CrudForm.apply

  def create(data: CrudForm.Data, host: User, arbiter: Option[User]): Fu[Simul] = {
    val simul = updateSimul(empty(host), data, host, arbiter)
    simulRepo create simul inject simul
  }

  def paginator(page: Int) = Paginator[Simul](adapter = new Adapter[Simul](
    collection = simulRepo.coll,
    selector = simulRepo.uniqueSelect,
    projection = $empty,
    sort = $doc("startsAt" -> -1)
  ), currentPage = page)

  private def empty(host: User) = Simul.make(
    host = host,
    clock = SimulClock(
      config = draughts.Clock.Config(0, 0),
      hostExtraTime = 0
    ),
    variants = Nil,
    color = DataForm.colorDefault,
    targetPct = None,
    ""
  )

  private def updateSimul(simul: Simul, data: CrudForm.Data, host: User, arbiter: Option[User]) = {
    import data._
    val clock = SimulClock(
      config = draughts.Clock.Config(clockTime * 60, clockIncrement),
      hostExtraTime = clockExtra * 60
    )
    val variantList = variants.flatMap { draughts.variant.Variant(_) }
    simul.copy(
      name = name,
      clock = clock,
      hostId = host.id,
      hostRating = host.perfs.bestRatingIn {
        variantList flatMap { variant =>
          lidraughts.game.PerfPicker.perfType(
            speed = draughts.Speed(clock.config.some),
            variant = variant,
            daysPerTurn = none
          )
        }
      },
      hostOfficialRating = host.profile.flatMap(_.fmjdRating),
      hostTitle = host.title,
      variants = variantList,
      color = color.some,
      targetPct = parseIntOption(percentage),
      arbiterId = arbiter map { _.id },
      spotlight = Spotlight(
        headline = headline,
        description = description,
        startsAt = date,
        homepageHours = homepageHours.some.filterNot(0 ==),
        iconImg = image.some.filter(_.nonEmpty),
        ceval = Simul.EvalSetting.byKey.get(ceval),
        fmjdRating = Simul.ShowFmjdRating.byKey.get(fmjd),
        drawLimit = parseIntOption(drawLimit),
        noAssistance = noAssistance.option(true),
        arbiterHidden = arbiterHidden.option(true),
        chatmode = Simul.ChatMode.byKey.get(chat)
      ).some
    )
  }
}
