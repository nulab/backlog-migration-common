package com.nulabinc.backlog.migration.common.dsl

import java.nio.file.Path

trait StorageDsl[F[_]] {

  def exists(path: Path): F[Boolean]
}
