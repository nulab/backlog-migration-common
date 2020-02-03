package com.nulabinc.backlog.migration.common.persistence.sqlite

sealed trait WriteType
case object Insert extends WriteType
case object Update extends WriteType
