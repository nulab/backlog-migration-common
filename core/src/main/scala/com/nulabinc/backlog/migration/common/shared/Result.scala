package com.nulabinc.backlog.migration.common.shared

import cats.data.EitherT
import monix.eval.Task


object Result {
  type Result[E, A] = Either[E, A]

  def success[A](a: A): Result[Nothing, A] = Right(a)
  def error[E, A](error: E): Result[E, A] = Left(error)
}

object AsyncResult {
  import Result.Result

  type AsyncResult[E, A] = Task[Result[E, A]]

  def success[A](a: A): AsyncResult[Nothing, A] = Task(Result.success(a))

  def error[E, A](error: E): AsyncResult[E, A] = Task(Result.error(error))

  def fromEither[E, A](result: Either[E, A]): AsyncResult[E, A] = Task(result)

  object syntax {

    implicit class AsyncResultOps[E, A](result: AsyncResult[E, A]) {
      def handleError: EitherT[Task, E, A] =
        EitherT(result)

      def mapError[E2](f: E => E2): AsyncResult[E2, A] =
        result.flatMap { inner =>
          inner.fold(
            error => AsyncResult.error(f(error)),
            data => AsyncResult.success(data)
          )
        }
    }

  }
}
