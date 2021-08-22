package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog4j.Priority

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class PriorityServiceImpl @Inject() (backlog: BacklogAPIClient) extends PriorityService {

  override def allPriorities(): Seq[Priority] =
    backlog.getPriorities.asScala.toSeq

}
