package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.{BacklogCustomStatus, BacklogProjectKey, BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.Project.CustomStatusColor
import com.nulabinc.backlog4j.api.option.{AddStatusParams, UpdateOrderOfStatusParams}
import javax.inject.Inject

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(backlog: BacklogAPIClient, projectKey: BacklogProjectKey) extends StatusService {

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
    backlog.updateOrderOfStatus(
      new UpdateOrderOfStatusParams(projectKey.value, ids.asJava)
    )

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
