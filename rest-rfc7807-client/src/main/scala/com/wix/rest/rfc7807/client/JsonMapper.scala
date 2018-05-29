package com.wix.rest.rfc7807.client

import java.util.TimeZone

import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.reflect.ClassTag


object JsonMapper {

  private val java8Modules = Seq(new Jdk8Module)
  private val defaultModules = Seq(new DefaultScalaModule, new JodaModule, new GuavaModule) ++ java8Modules


  def jsonStringTo[T](str: String)(implicit tag: ClassTag[T], mapper: ObjectMapper): T = {
    mapper.readValue(str, tag.runtimeClass.asInstanceOf[Class[T]])
  }


  object Implicits {

    implicit lazy val mapper: ObjectMapper =
      new ObjectMapper()
        .registerModules(defaultModules: _*)
        .disable(WRITE_DATES_AS_TIMESTAMPS)
        .setTimeZone(TimeZone.getTimeZone("UTC"))
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    implicit class JsonString2Object(s: String) {
      def as[T](implicit tag: ClassTag[T], mapper: ObjectMapper): T = jsonStringTo[T](s)
    }

  }

}
