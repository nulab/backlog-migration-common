package com.nulabinc.backlog.migration.common.dsl

trait AppDSL[F[_]] {
  def pure[A](a: A): F[A]
  def fromError[E, A](error: E): F[Either[E, A]]
}

object AppDSL {
  def apply[F[_]](implicit ev: AppDSL[F]): AppDSL[F] = ev
}
