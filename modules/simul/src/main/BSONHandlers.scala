package lidraughts.simul

import lidraughts.db.BSON
import lidraughts.db.dsl._
import reactivemongo.bson._

import draughts.Status
import draughts.variant.Variant

object BSONHandlers {

  private implicit val SimulStatusBSONHandler = new BSONHandler[BSONInteger, SimulStatus] {
    def read(bsonInt: BSONInteger): SimulStatus = SimulStatus(bsonInt.value) err s"No such simul status: ${bsonInt.value}"
    def write(x: SimulStatus) = BSONInteger(x.id)
  }
  private implicit val DraughtsStatusBSONHandler = lidraughts.game.BSONHandlers.StatusBSONHandler
  private implicit val VariantBSONHandler = new BSONHandler[BSONInteger, Variant] {
    def read(bsonInt: BSONInteger): Variant = Variant(bsonInt.value) err s"No such variant: ${bsonInt.value}"
    def write(x: Variant) = BSONInteger(x.id)
  }
  private implicit val ClockBSONHandler = {
    import draughts.Clock.Config
    implicit val clockHandler = Macros.handler[Config]
    Macros.handler[SimulClock]
  }
  private implicit val PlayerBSONHandler = Macros.handler[SimulPlayer]
  private implicit val ApplicantBSONHandler = Macros.handler[SimulApplicant]
  private implicit val SimulPairingBSONHandler = new BSON[SimulPairing] {
    def reads(r: BSON.Reader) = SimulPairing(
      player = r.get[SimulPlayer]("player"),
      gameId = r str "gameId",
      status = r.get[Status]("status"),
      wins = r boolO "wins",
      hostColor = r.strO("hostColor").flatMap(draughts.Color.apply) | draughts.White
    )
    def writes(w: BSON.Writer, o: SimulPairing) = $doc(
      "player" -> o.player,
      "gameId" -> o.gameId,
      "status" -> o.status,
      "wins" -> o.wins,
      "hostColor" -> o.hostColor.name
    )
  }
  import Simul.ChatMode
  private implicit val ChatModeHandler: BSONHandler[BSONString, ChatMode] = new BSONHandler[BSONString, ChatMode] {
    def read(bs: BSONString) = ChatMode.byKey get bs.value err s"Invalid chatmode ${bs.value}"
    def write(x: ChatMode) = BSONString(x.key)
  }
  import Simul.EvalSetting
  private implicit val EvalSettingHandler: BSONHandler[BSONString, EvalSetting] = new BSONHandler[BSONString, EvalSetting] {
    def read(bs: BSONString) = EvalSetting.byKey get bs.value err s"Invalid evalsetting ${bs.value}"
    def write(x: EvalSetting) = BSONString(x.key)
  }
  import Simul.ShowFmjdRating
  private implicit val ShowFmjdRatingHandler: BSONHandler[BSONString, ShowFmjdRating] = new BSONHandler[BSONString, ShowFmjdRating] {
    def read(bs: BSONString) = ShowFmjdRating.byKey get bs.value err s"Invalid fmjd setting ${bs.value}"
    def write(x: ShowFmjdRating) = BSONString(x.key)
  }
  private implicit val spotlightBSONHandler = Macros.handler[Spotlight]

  private[simul] implicit val SimulBSONHandler = Macros.handler[Simul]
}
