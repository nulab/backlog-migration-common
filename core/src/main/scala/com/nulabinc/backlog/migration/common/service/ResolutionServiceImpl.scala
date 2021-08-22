package com.nulabinc.backlog.migration.common.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import com.nulabinc.backlog4j.Resolution

import scala.jdk.CollectionConverters._

/**
 * @author
 *   uchida
 */
class ResolutionServiceImpl @Inject() (backlog: BacklogAPIClient) extends ResolutionService {

  override def allResolutions(): Seq[Resolution] =
    backlog.getResolutions.asScala.toSeq

}
