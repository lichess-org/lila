package lila.core

object hub:

  import lila.common.Bus

  final class RemoteService(channel: String):
    def register(f: PartialFunction[Matchable, Unit]): Unit = Bus.subscribeFun(channel)(f)
    def ask[A](using Executor, Scheduler)                   = Bus.ask[A](channel)

  val renderer = RemoteService("rs-renderer")
