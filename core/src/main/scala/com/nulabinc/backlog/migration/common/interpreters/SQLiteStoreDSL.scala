package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import com.nulabinc.backlog.migration.common.persistence.sqlite.DBIOTypes._
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import slick.jdbc.SQLiteProfile.api._

case class SQLiteStoreDSL(dbPath: Path)(implicit sc: Scheduler) extends StoreDSL[Task] {

  private val db = Database.forURL(
    s"jdbc:sqlite:${dbPath.toAbsolutePath}",
    driver = "org.sqlite.JDBC"
  )

  def read[A](a: DBIORead[A]): Task[A] =
    Task.deferFuture {
      db.run(a)
    }

  def write(a: DBIOWrite): Task[Int] =
    Task.deferFuture {
      db.run(a)
    }

  def createTable(a: DBIOSchema): Task[Unit] =
    Task.deferFuture {
      db.run(a).map(_ => ())
    }

  def stream[A](a: DBIOStream[A]): Task[Observable[A]] =
    Task.eval {
      Observable.fromReactivePublisher(
        db.stream(a)
      )
    }

}
