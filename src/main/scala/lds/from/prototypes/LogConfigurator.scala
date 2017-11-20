package lds.from.prototypes

import com.typesafe.scalalogging.Logger
import org.apache.log4j.spi.LoggerFactory
import org.apache.log4j.{Level => Log4jLevel, Logger => Log4jLogger}
import org.slf4j
import org.slf4j.impl.Log4jLoggerAdapter

import scala.collection.JavaConverters._
import scala.util.Try

case class IdProvider private(id: String) {
  val get = Try(id.substring(0, 8)).toOption.getOrElse(id)
}

object LogConfigurator {

  val logLevelPrefix = "logging_level_"

  val properties: Map[String, String] = System.getenv().asScala.toMap

  lazy val logger: Logger = Logger[this.type]

  def getLoggingProperties(properties: Map[String, String]): Map[String, String] =
    properties
      .filter { case (k, v) => k.startsWith(logLevelPrefix) }
      .map { case (k, v) => (k.replace(logLevelPrefix, ""), v) }
      .map { case (k, v) => (k.replace("_", "."), v) }

  def configureLog4jFromSystemProperties() {

    val logConfiguratorLog4JLogger = Log4jLogger
      .getLogger(LogConfigurator.getClass)

    // Get the original logging level for this classes logger.
    val originalLogLevel = logConfiguratorLog4JLogger.getLevel

    getLoggingProperties(properties).foreach { case (loggerName, logLevel) =>
      /**
        * Configure the configured logger, this may reconfigure
        * the logging level for this classes logger as it may be
        * a package specification.
        */
      Log4jLogger
        .getLogger(loggerName)
        .setLevel(Log4jLevel.toLevel(logLevel.toUpperCase))

      /**
       * Forcibly set this classes logger to INFO to guarantee
       * that we log the details of the log configuration that
       * we're applying.
       */
      logConfiguratorLog4JLogger.setLevel(Log4jLevel.INFO)
      logger.info(s"Configured logger $loggerName to log at $logLevel")
    }
    // Reset this classes logger to its original configuration level.
    logConfiguratorLog4JLogger.setLevel(originalLogLevel)

  }
}
