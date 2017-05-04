package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogEnvironment
import com.nulabinc.backlog.migration.utils.Logging
import com.nulabinc.backlog4j.Environment

/**
  * @author uchida
  */
class EnvironmentWrites @Inject()() extends Writes[Environment, BacklogEnvironment] with Logging {

  override def writes(environment: Environment): BacklogEnvironment = {
    BacklogEnvironment(
      name = environment.getName,
      spaceId = environment.getSpaceId
    )
  }

}
