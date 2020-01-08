package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.{BacklogCustomStatus, BacklogProjectKey, BacklogStatus, BacklogStatuses}
import com.nulabinc.backlog4j.BacklogAPIException
import com.nulabinc.backlog4j.Project.CustomStatusColor
import com.nulabinc.backlog4j.api.option.AddStatusParams
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

  override def add(status: BacklogCustomStatus): Unit =
    backlog.addStatus(
      new AddStatusParams(
        projectKey.value,
        status.name.trimmed,
        CustomStatusColor.strValueOf(status.color)
      )
    )

  private def defaultStatuses(): BacklogStatuses =
    BacklogStatuses(
      backlog
        .getStatuses
        .asScala
        .toSeq
        .map(BacklogStatus.from)
    )

}
