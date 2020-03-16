package com.nulabinc.backlog.migration.common.interpreters

import com.nulabinc.backlog.migration.common.dsl.AppDSL
import com.nulabinc.backlog.migration.common.shared.Result.Result
import monix.eval.Task

case class TaskAppDSL() extends AppDSL[Task] {

  override def pure[A](a: A): Task[A] =
    Task(a)

  override def fromError[E, A](error: E): Task[Result[E, A]] =
    Task(Left(error))
}
