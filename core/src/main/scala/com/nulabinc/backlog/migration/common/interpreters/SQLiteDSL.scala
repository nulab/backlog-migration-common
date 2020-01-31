package com.nulabinc.backlog.migration.common.interpreters

import java.nio.file.Path

import com.nulabinc.backlog.migration.common.dsl.DBIOTypes.{DBIORead, DBIOStream}
import com.nulabinc.backlog.migration.common.dsl.StoreDSL
import monix.eval.Task
import monix.reactive.Observable
import slick.jdbc.SQLiteProfile.api._

case class SQLiteDSL(dbPath: Path) extends StoreDSL[Task] {

  private val db = Database.forURL(s"jdbc:sqlite:${dbPath.toAbsolutePath}", driver = "org.sqlite.JDBC")

  def read[A](a: DBIORead[A]): Task[A] = Task.deferFuture {
    db.run(a)
  }

  def stream[A](a: DBIOStream[A]): Task[Observable[A]] = Task.eval {
    Observable.fromReactivePublisher(
      db.stream(a)
    )
  }

}
