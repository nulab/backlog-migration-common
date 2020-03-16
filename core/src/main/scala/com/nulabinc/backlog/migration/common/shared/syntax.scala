package com.nulabinc.backlog.migration.common.shared

import cats.Monad
import cats.data.EitherT

object syntax {

  implicit class ResultOps[F[_]: Monad, E, A](result: F[Either[E, A]]) {
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


}
