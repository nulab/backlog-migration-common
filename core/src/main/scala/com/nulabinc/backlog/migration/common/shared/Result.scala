package com.nulabinc.backlog.migration.common.shared

import cats.data.EitherT
import monix.eval.Task


object Result {

  type Result[E, A] = Task[Either[E, A]]

  def success[A](a: A): Result[Nothing, A] = Task(Right(a))

  def error[E, A](error: E): Result[E, A] = Task(Left(error))

  def fromEither[E, A](result: Either[E, A]): Result[E, A] = Task(result)

  object syntax {

    implicit class AsyncResultOps[E, A](result: Result[E, A]) {
      def handleError: EitherT[Task, E, A] =
        EitherT(result)

      def mapError[E2](f: E => E2): Result[E2, A] =
        result.flatMap { inner =>
          inner.fold(
            error => Result.error(f(error)),
            data => Result.success(data)
          )
        }
    }

    implicit class OptionTaskOps[A](optTaskValue: Option[Task[A]]) {
      def sequence: Task[Option[A]] =
        optTaskValue match {
          case Some(task) => task.map(Some(_))
          case None => Task(None)
        }
    }

  }
}
