package lila.message

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import scala.concurrent.duration._

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class MessageApi(
    coll: Coll,
    userRepo: UserRepo,
    threadRepo: ThreadRepo,
    shutup: lila.hub.actors.Shutup,
    maxPerPage: lila.common.config.MaxPerPage,
    relationApi: lila.relation.RelationApi,
    notifyApi: lila.notify.NotifyApi,
    security: MessageSecurity
) {

  import Thread.ThreadBSONHandler

  def inbox(me: User, page: Int): Fu[Paginator[Thread]] = Paginator(
    adapter = new Adapter(
      collection = coll,
      selector = threadRepo visibleByUserQuery me.id,
      projection = none,
      sort = threadRepo.recentSort
    ),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  private val unreadCountCache: AsyncLoadingCache[User.ID, Int] = Scaffeine()
    .expireAfterWrite(1 minute)
    .buildAsyncFuture[User.ID, Int](threadRepo.unreadCount _)

  def unreadCount(me: User): Fu[Int] = unreadCountCache.get(me.id)

  def thread(id: String, me: User): Fu[Option[Thread]] = for {
    threadOption <- coll.byId[Thread](id) map (_ filter (_ hasUser me))
    _ <- threadOption.filter(_ isUnReadBy me).??(threadRepo.setReadFor(me))
  } yield threadOption

  def sendPreset(mod: User, user: User, preset: ModPreset): Fu[Thread] =
    makeThread(
      DataForm.ThreadData(
        user = user,
        subject = preset.subject,
        text = preset.text,
        asMod = true
      ),
      mod
    )

  def sendPresetFromLichess(user: User, preset: ModPreset) =
    userRepo.lichess orFail "Missing lichess user" flatMap { sendPreset(_, user, preset) }

  def makeThread(data: DataForm.ThreadData, me: User): Fu[Thread] = {
    val fromMod = Granter(_.MessageAnyone)(me)
    userRepo named data.user.id flatMap {
      _.fold(fufail[Thread]("No such recipient")) { invited =>
        val t = Thread.make(
          name = data.subject,
          text = data.text,
          creatorId = me.id,
          invitedId = data.user.id,
          asMod = data.asMod
        )
        security.muteThreadIfNecessary(t, me, invited) flatMap { thread =>
          sendUnlessBlocked(thread, fromMod) flatMap {
            _ ?? {
              val text = s"${data.subject} ${data.text}"
              shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, invited.id, text, thread.looksMuted)
              notify(thread)
            }
          } inject thread
        }
      }
    }
  }

  private def sendUnlessBlocked(thread: Thread, fromMod: Boolean): Fu[Boolean] =
    if (fromMod) coll.insert.one(thread) inject true
    else relationApi.fetchBlocks(thread.invitedId, thread.creatorId) flatMap { blocks =>
      ((!blocks) ?? coll.insert.one(thread).void) inject !blocks
    }

  def makePost(thread: Thread, text: String, me: User): Fu[Thread] = {
    val post = Post.make(
      text = text,
      isByCreator = thread isCreator me
    )
    if (thread endsWith post) fuccess(thread) // prevent duplicate post
    else relationApi.fetchBlocks(thread receiverOf post, me.id) flatMap {
      case true => fuccess(thread)
      case false =>
        val newThread = thread + post
        coll.update.one($id(newThread.id), newThread) >> {
          val toUserId = newThread otherUserId me
          shutup ! lila.hub.actorApi.shutup.RecordPrivateMessage(me.id, toUserId, text, muted = false)
          notify(thread, post)
        } inject newThread
    }
  }

  def deleteThread(id: String, me: User): Funit =
    thread(id, me) flatMap {
      _ ?? { thread =>
        threadRepo.deleteFor(me.id)(thread.id) zip
          notifyApi.remove(
            lila.notify.Notification.Notifies(me.id),
            $doc("content.thread.id" -> thread.id)
          ) void
      }
    }

  def deleteThreadsBy(user: User): Funit =
    threadRepo.createdByUser(user.id) flatMap {
      _.map { thread =>
        val victimId = thread otherUserId user
        threadRepo.deleteFor(victimId)(thread.id) zip
          notifyApi.remove(
            lila.notify.Notification.Notifies(victimId),
            $doc("content.thread.id" -> thread.id)
          ) void
      }.sequenceFu.void
    }

  def notify(thread: Thread): Funit = thread.posts.headOption ?? { post =>
    notify(thread, post)
  }
  def notify(thread: Thread, post: Post): Funit =
    (thread isVisibleBy thread.receiverOf(post)) ?? {
      import lila.notify.{ Notification, PrivateMessage }
      import lila.common.String.shorten
      lila.common.Bus.publish(Event.NewMessage(thread, post), "newMessage")
      notifyApi addNotification Notification.make(
        Notification.Notifies(thread receiverOf post),
        PrivateMessage(
          PrivateMessage.SenderId(thread visibleSenderOf post),
          PrivateMessage.Thread(id = thread.id, name = shorten(thread.name, 80)),
          PrivateMessage.Text(shorten(post.text, 80))
        )
      )
    }

  def erase(user: User) = threadRepo.byAndForWithoutIndex(user) flatMap { threads =>
    lila.common.Future.applySequentially(threads) { thread =>
      coll.update.one($id(thread.id), thread erase user).void
    }
  }
}
