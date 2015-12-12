package lila.push

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.BSON._
import lila.db.Types.Coll
import lila.user.User

final class DeviceApi(coll: Coll) {

  private implicit val DeviceBSONHandler = Macros.handler[Device]

  private[push] def findByDeviceId(deviceId: String): Fu[Option[Device]] =
    coll.find(BSONDocument("_id" -> deviceId)).one[Device]

  private[push] def findByUserId(userId: String): Fu[List[Device]] =
    coll.find(BSONDocument("userId" -> userId)).cursor[Device]().collect[List]()

  def register(user: User, platform: String, deviceId: String) =
    coll.update(BSONDocument("_id" -> deviceId), Device(
      _id = deviceId,
      platform = platform,
      userId = user.id,
      seenAt = DateTime.now
    ), upsert = true).void
}
