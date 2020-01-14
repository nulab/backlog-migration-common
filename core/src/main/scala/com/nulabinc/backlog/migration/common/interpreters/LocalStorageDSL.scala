package com.nulabinc.backlog.migration.common.interpreters

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

import com.nulabinc.backlog.migration.common.dsl.StorageDSL
import monix.eval.Task
import monix.reactive.Observable

import scala.util.control.NonFatal

class LocalStorageDSL extends StorageDSL[Task] {

  override def readFile(path: Path): Task[String] = Task {
    new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  }

  override def writeFile(path: Path, content: String): Task[Unit] = {
    val stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
    val writeStream = Observable.fromInputStreamUnsafe(stream)

    for {
      _ <- delete(path)
      dir = path.toAbsolutePath.toFile.getParentFile
      dirExists <- exists(dir.toPath)
      _ = if (dirExists) () else dir.mkdir()
      _ <- write(path, writeStream, StandardOpenOption.CREATE)
    } yield ()
  }

  override def exists(path: Path): Task[Boolean] = Task {
    path.toFile.exists()
  }

  override def delete(path: Path): Task[Boolean] =
    exists(path).map { result =>
      if (result) {
        path.toFile.delete()
      } else {
        false
      }
    }

  private def write(path: Path, writeStream: Observable[Array[Byte]], option: StandardOpenOption): Task[Unit] =
    Task.deferAction { implicit scheduler =>
      Task.fromFuture {
        val os = Files.newOutputStream(path, option)
        writeStream.foreach { bytes =>
          os.write(bytes)
        }.map(_ => os.close())
          .recover {
            case NonFatal(ex) =>
              ex.printStackTrace()
              os.close()
          }
      }.map(_ => ())
    }
}
