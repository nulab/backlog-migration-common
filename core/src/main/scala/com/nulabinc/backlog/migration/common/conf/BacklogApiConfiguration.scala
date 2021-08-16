package com.nulabinc.backlog.migration.common.conf

import java.nio.file.{Path, Paths}

case class BacklogApiConfiguration(
    url: String,
    key: String,
    projectKey: String,
    backlogOutputPath: Path = Paths.get("./backlog")
) extends BacklogConfiguration {
  val isNAISpace: Boolean = url.contains(NaiSpaceDomain)
}
