package com.nulabinc.backlog.migration.common.interpreters

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{FileVisitOption, Files, Path, StandardOpenOption}
import java.util.Comparator

import com.nulabinc.backlog.migration.common.dsl.StorageDSL
import monix.eval.Task
import monix.reactive.Observable

import scala.util.Try
import scala.util.control.NonFatal

case class LocalStorageDSL() extends StorageDSL[Task] {

  private val charset = StandardCharsets.UTF_8

  override def read[A](path: Path, f: InputStream => A): Task[A] =
    Task.defer {
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
    val stream      = new ByteArrayInputStream(content.getBytes(charset))
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
      _ <- createDirectory(dir.toPath())
      _ <- write(path, stream, StandardOpenOption.CREATE)
    } yield ()

  override def writeAppendFile(
      path: Path,
      stream: Observable[Array[Byte]]
  ): Task[Unit] =
    write(path, stream, StandardOpenOption.APPEND)

  override def createDirectory(path: Path): Task[Boolean] =
    for {
      exists <- exists(path)
      result = if (exists) false else path.toFile().mkdir()
    } yield result

  override def exists(path: Path): Task[Boolean] =
    Task {
      path.toFile.exists()
    }

  override def delete(path: Path): Task[Unit] =
    exists(path).map { result =>
      if (result) {
        deleteRecursive(path)
      }
    }

  private def deleteRecursive(path: Path): Unit = {
    Files
      .walk(path, FileVisitOption.FOLLOW_LINKS)
      .sorted(Comparator.reverseOrder())
      .map(_.toFile)
      .peek(_ => ())
      .forEach(file => file.delete())
    ()
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
