package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.{BacklogProjectKey, BacklogStatus, BacklogStatuses}
import javax.inject.Inject

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(backlog: BacklogAPIClient, projectKey: BacklogProjectKey) extends StatusService {

  override def allStatuses(): BacklogStatuses =
    BacklogStatuses(
      backlog
        .getStatuses(projectKey)
        .asScala
        .toSeq
        .map(BacklogStatus.from)
    )
}
