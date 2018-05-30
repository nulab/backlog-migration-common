package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.client.BacklogAPIClient
import javax.inject.Inject
import com.nulabinc.backlog4j.Resolution

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class ResolutionServiceImpl @Inject()(backlog: BacklogAPIClient) extends ResolutionService {

  override def allResolutions(): Seq[Resolution] =
    backlog.getResolutions.asScala

}
