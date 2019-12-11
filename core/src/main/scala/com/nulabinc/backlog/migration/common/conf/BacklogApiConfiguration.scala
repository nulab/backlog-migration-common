package com.nulabinc.backlog.migration.common.conf

import java.nio.file.{Path, Paths}

/**
  * @author uchida
  */
case class BacklogApiConfiguration(url: String, key: String, projectKey: String, backlogOutputPath: Path = Paths.get("./backlog"))
