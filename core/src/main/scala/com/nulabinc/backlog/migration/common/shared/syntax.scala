package com.nulabinc.backlog.migration.common.shared

import cats.Monad
import cats.data.EitherT
import cats.implicits._

object syntax {

  implicit class ResultOps[F[_]: Monad, E, A](result: F[Either[E, A]]) {
    def handleError: EitherT[F, E, A] =
      EitherT(result)
  }

  implicit class ResultBooleanOps[F[_]: Monad](result: F[Boolean]) {
    def orError[E](error: E): F[Either[E, Unit]] =
      result.map {
        case false => Left(error)
        case true => Right(())
      }
  }

  implicit class EitherOps[F[_]: Monad, E, A](result: Either[E, A]) {
    def lift: F[Either[E, A]] = Result.fromEitherF(result)
  }

//    def mapError[E2](f: E => E2): F[Result[E2, A]] =
//      result.flatMap { inner =>
//        inner.fold(
//          error => Result.error(f(error)),
//          data => Result.success(data)
//        )
//      }

}
