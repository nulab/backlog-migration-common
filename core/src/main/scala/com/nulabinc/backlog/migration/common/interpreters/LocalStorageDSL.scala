package com.nulabinc.backlog.migration.common.interpreters

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}

import com.nulabinc.backlog.migration.common.dsl.StorageDSL
import monix.eval.Task
import monix.reactive.Observable

import scala.util.Try
import scala.util.control.NonFatal

case class LocalStorageDSL() extends StorageDSL[Task] {

  private val charset = StandardCharsets.UTF_8

  override def read[A](path: Path, f: InputStream => A): Task[A] =
    Task.deferAction { implicit scheduler =>
      Task.eval {
        val is = Files.newInputStream(path)
        Try(f(is))
          .map { result =>
            is.close()
            result
          }
          .recover {
            case NonFatal(ex) =>
              is.close()
              throw ex
          }
          .get
      }
    }

  override def writeFile(path: Path, content: String): Task[Unit] = {
    val stream = new ByteArrayInputStream(content.getBytes(charset))
    val writeStream = Observable.fromInputStreamUnsafe(stream)

    writeNewFile(path, writeStream)
  }

  override def writeNewFile(
      path: Path,
      stream: Observable[Array[Byte]]
  ): Task[Unit] =
    for {
      _ <- delete(path)
      dir = path.toAbsolutePath.toFile.getParentFile
      dirExists <- exists(dir.toPath)
      _ = if (dirExists) () else dir.mkdir()
      _ <- write(path, stream, StandardOpenOption.CREATE)
    } yield ()

  override def writeAppendFile(
      path: Path,
      stream: Observable[Array[Byte]]
  ): Task[Unit] =
    write(path, stream, StandardOpenOption.APPEND)

  override def exists(path: Path): Task[Boolean] =
    Task {
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

  private def write(
      path: Path,
      writeStream: Observable[Array[Byte]],
      option: StandardOpenOption
  ): Task[Unit] =
    Task.deferAction { implicit scheduler =>
      Task
        .fromFuture {
          val os = Files.newOutputStream(path, option)
          writeStream
            .foreach { bytes =>
              os.write(bytes)
            }
            .map(_ => os.close())
            .recover {
              case NonFatal(ex) =>
                ex.printStackTrace()
                os.close()
            }
        }
        .map(_ => ())
    }
}
