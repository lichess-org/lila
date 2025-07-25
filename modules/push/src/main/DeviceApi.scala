package lila.push

import play.api.mvc.RequestHeader
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class DeviceApi(coll: Coll)(using Executor):

  private given BSONDocumentHandler[Device] = Macros.handler

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    coll.delete.one($doc("userId" -> del.id)).void

  private[push] def findByDeviceId(deviceId: String): Fu[Option[Device]] =
    coll.find($id(deviceId)).one[Device]

  private[push] def findLastManyByUserId(platform: String, max: Int)(userId: UserId): Fu[List[Device]] =
    coll
      .find(
        $doc(
          "platform" -> platform,
          "userId" -> userId
        )
      )
      .sort($doc("seenAt" -> -1))
      .cursor[Device]()
      .list(max)

  private[push] def findLastOneByUserId(platform: String)(userId: UserId): Fu[Option[Device]] =
    findLastManyByUserId(platform, 1)(userId).dmap(_.headOption)

  def register(user: User, platform: String, deviceId: String)(using req: RequestHeader) =
    lila.mon.push.register.in(platform).increment()
    coll.update
      .one(
        $id(deviceId),
        Device(
          _id = deviceId,
          platform = platform,
          userId = user.id,
          seenAt = nowInstant,
          ua = lila.common.HTTPRequest.userAgent(req)
        ),
        upsert = true
      )
      .void

  def unregister(user: User) =
    lila.mon.push.register.out.increment()
    coll.delete.one($doc("userId" -> user.id)).void

  def delete(device: Device) =
    coll.delete.one($id(device._id)).void
