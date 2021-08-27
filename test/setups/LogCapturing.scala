/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package setups

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger ⇒ LogbackLogger}
import ch.qos.logback.core.read.ListAppender
import play.api.LoggerLike

import scala.collection.JavaConverters._
trait LogCapturing {

  private def withCaptureOfLoggingFrom(logger: LogbackLogger)(body: (=> List[ILoggingEvent]) => Unit) {
    val appender = new ListAppender[ILoggingEvent]()
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.ALL)
    logger.setAdditive(true)
    body(appender.list.asScala.toList)
  }

  def withCaptureOfLoggingFrom(logger: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit) {
    withCaptureOfLoggingFrom(logger.logger.asInstanceOf[LogbackLogger])(body)
  }
}
