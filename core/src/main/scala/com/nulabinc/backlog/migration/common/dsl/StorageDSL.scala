package com.nulabinc.backlog.migration.common.dsl

import java.io.InputStream
import java.nio.file.Path

import monix.reactive.Observable

trait StorageDSL[F[_]] {

  def read[A](path: Path, f: InputStream => A): F[A]

  def writeFile(path: Path, content: String): F[Unit]

  def writeNewFile(path: Path, stream: Observable[Array[Byte]]): F[Unit]

  def writeAppendFile(path: Path, stream: Observable[Array[Byte]]): F[Unit]

  def createDirectory(path: Path): F[Boolean]

  def exists(path: Path): F[Boolean]

  def delete(path: Path): F[Unit]

}

object StorageDSL {
  def apply[F[_]](implicit ev: StorageDSL[F]): StorageDSL[F] = ev
}
