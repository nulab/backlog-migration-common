package com.nulabinc.backlog.migration.importer.core

import com.nulabinc.backlog.migration.common.dsl.ConsoleDSL
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException
import monix.eval.Task
import monix.execution.Scheduler

case class RetryException(throwables: List[Throwable]) extends Exception(throwables.toString())

/*
  Thank you: http://d.hatena.ne.jp/j5ik2o/20120627/1340748199#20120627fn1
 */
object RetryUtil extends Logging {

  import scala.util.control.Exception.allCatch

  def retry[T](retryLimit: Int)(f: => T)(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T =
    retry(retryLimit, 0, classOf[Throwable])(f)

  def retry[T](retryLimit: Int, retryInterval: Int)(
      f: => T
  )(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T =
    retry(retryLimit, retryInterval, classOf[Throwable])(f)

  def retry[T](retryLimit: Int, catchExceptionClasses: Class[_]*)(
      f: => T
  )(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T =
    retry(
      retryLimit,
      0,
      e => catchExceptionClasses.exists(_.isAssignableFrom(e.getClass))
    )(f)

  def retry[T](retryLimit: Int, shouldCatch: Throwable => Boolean)(
      f: => T
  )(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T =
    retry(retryLimit, 0, shouldCatch)(f)

  def retry[T](
      retryLimit: Int,
      retryInterval: Int,
      catchExceptionClasses: Class[_]*
  )(f: => T)(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T =
    retry(
      retryLimit,
      retryInterval,
      e => catchExceptionClasses.exists(_.isAssignableFrom(e.getClass))
    )(f)

  def retryBacklogAPIException[T](
      retryLimit: Int,
      retryInterval: Int
  )(f: => T)(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T =
    retry(
      retryLimit = retryLimit,
      retryInterval = retryInterval,
      shouldCatch = e =>
        e match {
          case ex: BacklogAPIException =>
            !ex.getMessage.contains("No comment content")
          case _ =>
            true
        }
    )(f)

  def retry[T](
      retryLimit: Int,
      retryInterval: Int,
      shouldCatch: Throwable => Boolean
  )(f: => T)(implicit consoleDSL: ConsoleDSL[Task], s: Scheduler): T = {
    @annotation.tailrec
    def retry0(errors: List[Throwable], f: => T): T = {
      allCatch.either(f) match {
        case Right(r) => r
        case Left(e) =>
          if (shouldCatch(e)) {
            if (retryLimit > 0) {
              val message =
                s"(${errors.size + 1} / $retryLimit) Retrying... ${e.getMessage}"
              ConsoleDSL[Task].warning(message).runSyncUnsafe()
            }
            if (errors.size < retryLimit - 1) {
              Thread.sleep(retryInterval)
              retry0(e :: errors, f)
            } else {
              throw RetryException(e :: errors)
            }
          } else throw e
      }
    }
    retry0(Nil, f)
  }

}
