package org.silkframework.config

import java.io.File
import java.lang.management.ManagementFactory
import java.util.logging.Logger

import javax.inject.Named
import Config._
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

import scala.collection.JavaConverters._

/**
  * Holds the configuration properties
  */
trait Config {
  /** Returns an instance of the current [[TypesafeConfig]] */
  def apply(): TypesafeConfig

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  def refresh(): Unit
}

object Config{
  final val ELDS_HOME_ENV: String = "ELDS_HOME"
  final val ELDS_HOME_CONF: String = "elds.home"
  final val USER_HOME_CONF: String = "user.home"
  final val DATAINTEGRATION_PATH: String = "/etc/dataintegration"
  final val DATAINTEGRATION_CONFIG_DIR: String = DATAINTEGRATION_PATH + "/conf"
  final val DATAINTEGRATION_CONF: String = "/conf/dataintegration.conf"
  final val REFERENCE_CONF: String = "/conf/reference.conf"
  final val APPLICATION_CONF: String = "/conf/application.conf"
}

@Named("default")
class DefaultConfig private() extends Config {
  // Overwrite default logging pattern for java.util.logging
  if (System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %3$s%n%4$s: %5$s%6$s%n")
  }

  private lazy val log = Logger.getLogger(this.getClass.getName)

  private var config = this.synchronized {init()}

  /**
    * Will check and return if ELDS_HOME was defined either as environment variable or in the dataintegration config.
    */
  lazy val eldsHomeDir: Option[File] ={
    ConfigFactory.invalidateCaches()
    val conf = ConfigFactory.load()
    Option(if(conf.hasPath(ELDS_HOME_CONF)) conf.getString(ELDS_HOME_CONF) else System.getenv(ELDS_HOME_ENV))
      .map(p => new File(p))
  }

  private def init(): TypesafeConfig = {
    this.synchronized {
      ConfigFactory.invalidateCaches()
      val initialSystemConfig = ConfigFactory.load()
      // Check if we are running as part of the eccenca Linked Data Suite
      var fullConfig: TypesafeConfig = eldsHomeDir match {
        case Some(eldsHome) =>
          val dataintegrationConfigPath = DATAINTEGRATION_PATH + DATAINTEGRATION_CONF
          val configFile = new File(eldsHome + dataintegrationConfigPath)
          if (!configFile.exists) {
            val msg = new StringBuilder
            msg ++= s"Configuration file not found at: ${configFile.getAbsolutePath}.\n"
            msg ++= s"Falling back on default reference.conf file.\n"
            msg ++= "Possible fix: Map a volume with the config file to this location.\n"
            msg ++= "Otherwise set elds.home or $ELDS_HOME to point to the correct location."
            log.warning(msg.toString())
          }
          initialSystemConfig.withFallback(ConfigFactory.parseFile(configFile))
        case None => Logger.getLogger(this.getClass.getName).info(
          "Variable $ELDS_HOME is not defined. If this application is not running in the ELDS context " +
            "you can ignore this warning. Otherwise please configure $ELDS_HOME or elds.home."
          )
          initialSystemConfig
      }

      // Check if we are running as part of the Play Framework
      val referenceConf = new File(System.getProperty(USER_HOME_CONF) + REFERENCE_CONF)
      val applicationConf = new File(System.getProperty(USER_HOME_CONF) + APPLICATION_CONF)
      if (applicationConf.exists()) {
        //fallback to play definition
        fullConfig = fullConfig.withFallback(ConfigFactory.parseFile(applicationConf))
      }
      if (referenceConf.exists()) {
        // fallback to reference conf
        fullConfig = fullConfig.withFallback(ConfigFactory.parseFile(referenceConf))
      }
      // resolve all parameters
      val finalConfig = fullConfig.resolve()
      // publish everything to the system
      finalConfig.entrySet().asScala.foreach(e => System.setProperty(e.getKey, e.getValue.unwrapped().toString))
      // finally we re commit any specific command line argument which may have been overwritten by the property sources
      DefaultConfig.reapplyCommandLineProperties()
      finalConfig
    }
  }

  def apply(): TypesafeConfig = {
    this.synchronized {
      config
    }
  }

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  override def refresh(): Unit = {
    this.synchronized {
      config = init()
    }
  }
}

object DefaultConfig {
  // This default initialization needed for usages that don't involve dependency injection
  lazy val instance = new DefaultConfig()


  private val CLARegex = """-D\s*([^=]+)\s*=\s*(.+)""".r
  /**
    * Will re-commit any specifically provided command line properties, in case they were overwritten by other loaded property sources.
    */
  def reapplyCommandLineProperties(): Unit ={
    ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.foreach{a => CLARegex.findFirstMatchIn(a) match{
      case Some(m) => System.setProperty(m.group(1), m.group(2).trim)
      case None =>
    }
    }
  }
}
