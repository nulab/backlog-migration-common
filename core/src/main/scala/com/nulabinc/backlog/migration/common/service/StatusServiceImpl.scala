package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.{BacklogCustomStatus, BacklogProjectKey, BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.Project.CustomStatusColor
import com.nulabinc.backlog4j.api.option.{AddStatusParams, UpdateOrderOfStatusParams}
import javax.inject.Inject

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(backlog: BacklogAPIClient,
                                  projectKey: BacklogProjectKey) extends StatusService with Logging {

  override def allStatuses(): BacklogStatuses =
    Try {
      BacklogStatuses(
        backlog
          .getStatuses(projectKey)
          .asScala
          .toSeq
          .map(BacklogStatus.from)
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

  override def updateOrder(ids: Seq[Int]): Unit =
    Try {
      backlog.updateOrderOfStatus(
        new UpdateOrderOfStatusParams(projectKey.value, ids.asJava)
      )
    }.recover {
      case ex: BacklogAPIException if ex.getMessage.contains("Undefined resource") =>
        logger.warn("Your backlog doesn't support the updateOrder API", ex)
      case ex =>
        logger.error(s"UpdateOrder API error. Message: ${ex.getMessage}", ex)
        throw ex
    }.getOrElse(())

  override def remove(id: Int): Unit =
    backlog.removeStatus(projectKey.value, id, 1) // Any status id is OK

  private def defaultStatuses(): BacklogStatuses =
    BacklogStatuses(
      backlog
        .getStatuses
        .asScala
        .toSeq
        .map(BacklogStatus.from)
    )

}
