package com.nulabinc.backlog.migration.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.convert.Writes
import com.nulabinc.backlog.migration.domain.BacklogUser
import com.nulabinc.backlog.migration.utils.Logging

/**
  * @author uchida
  */
class UserNameWrites @Inject()() extends Writes[String, BacklogUser] with Logging {

  override def writes(name: String): BacklogUser = {
    BacklogUser(None, None, None, name, None, BacklogConstantValue.USER_ROLE)
  }

}
