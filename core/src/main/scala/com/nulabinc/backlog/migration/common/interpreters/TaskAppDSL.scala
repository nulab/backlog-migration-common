package com.nulabinc.backlog.migration.common.interpreters

import com.nulabinc.backlog.migration.common.dsl.AppDSL
import monix.eval.Task

case class TaskAppDSL() extends AppDSL[Task] {

  override def pure[A](a: A): Task[A] =
    Task(a)

  override def fromError[E, A](error: E): Task[Either[E, A]] =
    Task(Left(error))
}
