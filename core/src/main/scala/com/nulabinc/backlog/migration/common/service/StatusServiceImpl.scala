package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.{
  BacklogCustomStatus,
  BacklogProjectKey,
  BacklogStatus,
  BacklogStatuses,
  Id
}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.Project.CustomStatusColor
import com.nulabinc.backlog4j.api.option.{AddStatusParams, UpdateOrderOfStatusParams}

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
 * @author uchida
 */
class StatusServiceImpl @Inject() (
    backlog: BacklogAPIClient,
    projectKey: BacklogProjectKey
) extends StatusService
    with Logging {

  override def allStatuses(): BacklogStatuses =
    Try {
      BacklogStatuses(
        backlog.getStatuses(projectKey).asScala.toSeq.map(BacklogStatus.from)
      )
    }.recover {
      case ex: BacklogAPIException if ex.getMessage.contains("No such project") =>
        defaultStatuses()
      case ex =>
        throw ex
    }.getOrElse(defaultStatuses())

  override def add(status: BacklogCustomStatus): BacklogCustomStatus = {
    val added = backlog.addStatus(
      new AddStatusParams(
        projectKey.value,
        status.name.trimmed,
        CustomStatusColor.strValueOf(status.color)
      )
    )

    BacklogCustomStatus.from(added)
  }

  override def updateOrder(ids: Seq[Id[BacklogStatus]]): Unit =
    Try {
      backlog.updateOrderOfStatus(
        new UpdateOrderOfStatusParams(projectKey.value, ids.map(_.value).asJava)
      )
    }.recover {
      case ex: BacklogAPIException if ex.getMessage.contains("Undefined resource") =>
        logger.warn("Your backlog doesn't support the updateOrder API", ex)
      case ex =>
        logger.error(s"UpdateOrder API error. Message: ${ex.getMessage}", ex)
        throw ex
    }.getOrElse(())

  override def remove(id: Id[BacklogStatus]): Unit =
    backlog.removeStatus(projectKey.value, id.value, 1) // Any status id is OK

  private def defaultStatuses(): BacklogStatuses =
    BacklogStatuses(
      backlog.getStatuses.asScala.toSeq.map(BacklogStatus.from)
    )

}
