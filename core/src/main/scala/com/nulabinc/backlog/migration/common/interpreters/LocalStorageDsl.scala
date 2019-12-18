package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.dsl.StorageDsl
import monix.eval.Task

class LocalStorageDsl extends StorageDsl[Task] {

  override def exists(path: Path): Task[Boolean] = Task {
    path.toFile.exists()
  }

}
