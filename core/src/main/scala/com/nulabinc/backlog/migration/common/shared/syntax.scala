package com.nulabinc.backlog.migration.common.shared

import cats.Monad
import cats.data.EitherT
import com.nulabinc.backlog.migration.common.shared.Result.Result
import monix.eval.Task

object syntax {

  implicit class AsyncResultOps[F[_]: Monad, E, A](result: F[Result[E, A]]) {
    def handleError: EitherT[F, E, A] =
      EitherT(result)

//    def mapError[E2](f: E => E2): F[Result[E2, A]] =
//      result.flatMap { inner =>
//        inner.fold(
//          error => Result.error(f(error)),
//          data => Result.success(data)
//        )
//      }
  }

  implicit class OptionTaskOps[A](optTaskValue: Option[Task[A]]) {
    def sequence: Task[Option[A]] =
      optTaskValue match {
        case Some(task) => task.map(Some(_))
        case None => Task(None)
      }
  }
}
