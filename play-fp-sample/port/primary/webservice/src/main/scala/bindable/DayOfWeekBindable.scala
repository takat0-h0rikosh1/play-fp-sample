package bindable

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

import cats.data.EitherT
import cats.implicits._
import play.api.mvc.QueryStringBindable

import scala.util.Try

trait DayOfWeekBindable {

  private type Bindable[A] = QueryStringBindable[A]
  private type Params = Map[String, Seq[String]]
  private type Bound[A] = Option[Either[String, A]]

  implicit def dayOfWeekBinder(
      implicit binder: Bindable[String]): Bindable[DayOfWeek] =
    new Bindable[DayOfWeek] {

      override def bind(key: String, params: Params): Bound[DayOfWeek] = {
        val eitherT = for {
          str <- EitherT(binder.bind("dayOfWeek", params))
          dayOfWeek <- EitherT(
            Option(
              Try(DayOfWeek.valueOf(str.toUpperCase)).toEither
                .leftMap(_.getMessage)))
        } yield dayOfWeek
        eitherT.value
      }

      override def unbind(key: String, value: DayOfWeek): String =
        binder.unbind("dayOfWeek",
                      value.getDisplayName(TextStyle.FULL, Locale.JAPAN))

    }

}
