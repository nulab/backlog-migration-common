package com.nulabinc.backlog.migration.common.shared

import monix.eval.Task

object syntax {

  implicit class OptionTaskOps[A](optTaskValue: Option[Task[A]]) {
    def sequence: Task[Option[A]] =
      optTaskValue match {
        case Some(task) => task.map(Some(_))
        case None => Task(None)
      }
  }
}
