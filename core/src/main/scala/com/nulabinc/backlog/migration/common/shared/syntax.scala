package com.nulabinc.backlog.migration.common.shared

import cats.data.EitherT
import cats.implicits._
import cats.{Applicative, Monad}

object syntax {

  implicit class ResultOps[F[_]: Monad, E, A](result: F[Either[E, A]]) {
    def handleError: EitherT[F, E, A] =
      EitherT(result)

    def mapError[E2](f: E => E2): F[Either[E2, A]] =
      result.map { inner =>
        inner.fold(
          error => Left(f(error)),
          data => Right(data)
        )
      }
  }

  implicit class ResultBooleanOps[F[_]: Monad](result: F[Boolean]) {
    def orError[E](error: E): F[Either[E, Unit]] =
      result.map {
        case false => Left(error)
        case true  => Right(())
      }
    def lift[E]: EitherT[F, E, Unit] =
      EitherT(result.map(_ => Right(())))
  }

  implicit class EitherOps[F[_]: Monad, E, A](result: Either[E, A]) {
    def lift: F[Either[E, A]] =
      Applicative[F].pure(result)

    def orFail: A =
      result match {
        case Right(value) => value
        case Left(error)  => throw new RuntimeException(error.toString)
      }
  }

  implicit class OptionOps[F[_]: Monad, A](optFValue: Option[F[A]]) {
    def sequence: F[Option[A]] =
      optFValue match {
        case Some(task) => task.map(Some(_))
        case None       => Applicative[F].pure(None)
      }

    def toEither[E](error: E): F[Either[E, A]] =
      optFValue match {
        case Some(v) => v.map(Right(_))
        case None    => Applicative[F].pure(Left(error))
      }
  }

  implicit class UnitOps[F[_]: Monad](value: F[Unit]) {
    def lift[E]: EitherT[F, E, Unit] =
      EitherT(value.map(Right(_)))
  }

  implicit class SeqOps[F[_]: Monad, A](values: Seq[F[A]]) {
    def sequence: F[Seq[A]] =
      values.foldLeft(Applicative[F].pure(Seq.empty[A])) { (itemsF, acc) =>
        acc.flatMap { item =>
          itemsF.map(items => item +: items)
        }
      }
  }
}
