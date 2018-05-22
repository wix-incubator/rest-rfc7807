package com.wix.rest.rfc7807.client.exception


case class CommunicationException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)
