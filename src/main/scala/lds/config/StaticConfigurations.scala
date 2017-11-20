package lds.config

object StaticConfigurations {

  private val configMap: Map[String, Seq[String]] = Map(
    "private" -> Seq(
      ".*Fesource.*",
      ".*hesource.*",
      ".*Resource.*"
    ), "public" -> Seq(
      ".*hesource.*",
      ".*Resource.*"
    )
  )

  def get(configName: String): Option[Seq[String]] = configMap.get(configName)

}
