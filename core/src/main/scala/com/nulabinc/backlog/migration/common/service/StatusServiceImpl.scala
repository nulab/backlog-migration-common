package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog.migration.common.domain.BacklogProjectKey
import javax.inject.Inject
import com.nulabinc.backlog4j.Status

import scala.jdk.CollectionConverters._

/**
  * @author uchida
  */
class StatusServiceImpl @Inject()(backlog: BacklogAPIClient, projectKey: BacklogProjectKey) extends StatusService {

  override def allStatuses(): Seq[Status] =
    backlog.getStatuses(projectKey).asScala.toSeq

}
