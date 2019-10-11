package bindable

import java.time.{Clock, ZonedDateTime}

import cats.data.EitherT
import cats.implicits._
import controllers.Interval
import play.api.mvc.QueryStringBindable

trait IntervalBindable {

  private type Bindable[A] = QueryStringBindable[A]
  private type Params = Map[String, Seq[String]]
  private type Bound[A] = Option[Either[String, A]]
  private type ZDT = ZonedDateTime

  private implicit lazy val clock: Clock = Clock.systemDefaultZone()

  implicit def intervalBinder(
      implicit binder: Bindable[Long]): Bindable[Interval] =
    new Bindable[Interval] {

      override def bind(key: String, params: Params): Bound[Interval] =
        Some({
          implicit val p: Params = params
          for {
            from <- bindToRightOption[Long]("from")
            to <- bindToRightOption[Long]("to")
          } yield Interval(from, to)
        })

      override def unbind(key: String, value: Interval): String =
        Seq(
          binder.unbind("from", value.from.toInstant.toEpochMilli),
          binder.unbind("to", value.from.toInstant.toEpochMilli)
        ).mkString("&")

      private def bindToRightOption[A](key: String)(
          implicit params: Params,
          bindable: Bindable[A]): Either[String, Option[A]] =
        EitherT(bindable.bind(key, params))
          .map(Option(_))
          .value
          .getOrElse(Right(None))

    }

}
