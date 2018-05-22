package com.wix.rest.rfc7807.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{RequestTransformer, WithTransformerConcatenation}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, StreamTcpException}
import com.wix.rest.rfc7807.api.model.ErrorResponse
import com.wix.rest.rfc7807.client.JsonMapper.Implicits.{JsonString2Object, mapper}
import com.wix.rest.rfc7807.client.exception.{CommunicationException, ProtocolException}

import scala.concurrent.{ExecutionContext, Future}


class AkkaRestClient(errorResponseAsException: ErrorResponse => RuntimeException)
                    (implicit system: ActorSystem, executionContext: ExecutionContext) {

  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val http = Http()


  def execute(request: HttpRequest): ExecuteResponseContext = {
    val pipeline: WithTransformerConcatenation[HttpRequest, Future[Either[String, ErrorResponse]]] =
      addJsonContentTypeHeader ~>
        sendReceive ~>
        unmarshal
    new ExecuteResponseContext(pipeline(request))
  }

  private def addJsonContentTypeHeader(): RequestTransformer = { request =>
    request.mapEntity({
      case entity if entity == HttpEntity.Empty => entity
      case entity => entity.withContentType(ContentTypes.`application/json`)
    })
  }

  private def sendReceive(request: HttpRequest): Future[HttpResponse] = http.singleRequest(request)

  private def unmarshal(response: HttpResponse): Future[Either[String, ErrorResponse]] = {
    response.status match {
      case ServerError() | ClientError() => Unmarshal(response.entity).to[String].map(str => Right(str.as[ErrorResponse]))
      case _ => Unmarshal(response.entity).to[String].map(Left(_))
    }
  }


  class ExecuteResponseContext(executeResponse: Future[Either[String, ErrorResponse]]) {

    def withResult[T]()(implicit mn: Manifest[T]): Future[T] = {
      extractResponse(responseFromStringConverter = _.as[T])
    }

    def withoutResult(): Future[Unit] = {
      extractResponse(responseFromStringConverter = _ => Unit)
    }


    private def extractResponse[T](responseFromStringConverter: String => T) = {
      executeResponse map {
        case HasValue(response) => Left(responseFromStringConverter(response))
        case HasError(errorResponse) => Right(errorResponse)
      } recover {
        case err: StreamTcpException => throw CommunicationException(err.getMessage, err)
        case err => throw ProtocolException(err.getMessage, err)
      } map {
        case HasValue(response) => response
        case HasError(errorResponse) => throw errorResponseAsException(errorResponse)
      }
    }

  }

}


object HasValue {
  def unapply[T](res: Either[T, ErrorResponse]): Option[T] = res.left.toOption
}

object HasError {
  def unapply[T](res: Either[T, ErrorResponse]): Option[ErrorResponse] = res.right.toOption
}

object ClientError {
  def unapply(status: StatusCode): Boolean = {
    status.intValue >= 400 && status.intValue < 500
  }
}

object ServerError {
  def unapply(status: StatusCode): Boolean = {
    status.intValue >= 500 && status.intValue < 600
  }
}

