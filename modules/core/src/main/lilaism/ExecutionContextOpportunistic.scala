package scala.concurrent:
  /*
   * Asynchronous code with short-lived nested tasks is executed more efficiently when using
   * `ExecutionContext.opportunistic` (continue reading to learn why it is `private[scala]` and how to access it).
   *
   * `ExecutionContext.opportunistic` uses the same thread pool as `ExecutionContext.global`. It attempts to batch
   * nested task and execute them on the same thread as the enclosing task. This is ideally suited to execute
   * short-lived tasks as it reduces the overhead of context switching.
   *
   * WARNING: long-running and/or blocking tasks should be demarcated within [[scala.concurrent.blocking]]-blocks
   *          to ensure that any pending tasks in the current batch can be executed by another thread on `global`.
   *
   * See ExecutionContext documentation for more information.
   */
  val ExecutionContextOpportunistic: ExecutionContextExecutor =
    ExecutionContext.opportunistic
