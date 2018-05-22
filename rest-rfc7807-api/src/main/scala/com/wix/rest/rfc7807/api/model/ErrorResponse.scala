package com.wix.rest.rfc7807.api.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include


@JsonInclude(Include.NON_NULL)
case class ErrorResponse(`type`: String, title: String, detail: Option[String] = None)
