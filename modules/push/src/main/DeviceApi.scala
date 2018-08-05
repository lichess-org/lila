package lila.push

import org.joda.time.DateTime

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

private final class DeviceApi(coll: Coll) {

  private implicit val DeviceBSONHandler = Macros.handler[Device]

  private[push] def findByDeviceId(deviceId: String): Fu[Option[Device]] =
    coll.find($id(deviceId)).one[Device]

  private[push] def findByUserId(userId: String): Fu[List[Device]] =
    coll.find($doc("userId" -> userId)).cursor[Device]().list

  private[push] def findLastManyByUserId(platform: String, max: Int)(userId: String): Fu[List[Device]] =
    coll.find($doc(
      "platform" -> platform,
      "userId" -> userId
    )).sort($doc("seenAt" -> -1)).cursor[Device]().list(max)

  private[push] def findLastOneByUserId(platform: String)(userId: String): Fu[Option[Device]] =
    findLastManyByUserId(platform, 1)(userId) map (_.headOption)

  def register(user: User, platform: String, deviceId: String): Funit = {
    lila.mon.push.register.in(platform)()

    coll.update.one(
      $id(deviceId),
      u = Device(
        _id = deviceId,
        platform = platform,
        userId = user.id,
        seenAt = DateTime.now
      ), upsert = true
    ).void
  }

  def unregister(user: User): Funit = {
    lila.mon.push.register.out()

    coll.delete.one($doc("userId" -> user.id)).void
  }
}
